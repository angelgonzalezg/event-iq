package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.eventiq.OracleTestDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.jpa.hibernate.ddl-auto=validate", "spring.ai.model.chat=none", "spring.ai.google.genai.api-key=unused" })
@Import(OracleTestDatabase.class)
class AttendeeServiceIT {

	@LocalServerPort
	private int port;

	@Autowired
	private AttendeeService service;

	@Autowired
	private AttendeeRepository attendees;

	private final String email = "attendee-" + UUID.randomUUID() + "@example.com";

	@AfterEach
	void removeCommittedTestData() {
		this.attendees.findByEmail(this.email).ifPresent(this.attendees::delete);
	}

	@Test
	void commitsTheCanonicalEmailAndFindsItRegardlessOfCaseOrOuterSpaces() {
		String suppliedEmail = "  " + this.email.toUpperCase(Locale.ROOT) + "  ";
		AttendeeResponse created = this.service.create(new CreateAttendeeRequest("Ana O'León", suppliedEmail, null));

		assertThat(created.attendeeId()).isPositive();
		assertThat(created.email()).isEqualTo(this.email);
		assertThat(this.service.findByEmail(suppliedEmail)).isEqualTo(created);
		assertThat(this.attendees.findById(created.attendeeId()).orElseThrow().getEmail()).isEqualTo(this.email);
	}

	@Test
	void rejectsACaseVariantDuplicateWithoutChangingTheOriginalAttendee() {
		AttendeeResponse original = this.service.create(new CreateAttendeeRequest("Ana", this.email, null));
		CreateAttendeeRequest duplicate = new CreateAttendeeRequest("Another Ana", " " + this.email.toUpperCase(Locale.ROOT) + " ", "123");

		assertThatExceptionOfType(ResponseStatusException.class)
			.isThrownBy(() -> this.service.create(duplicate))
			.satisfies(error -> {
				assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(error.getReason()).isEqualTo("An attendee with this email already exists.");
			});
		assertThat(this.service.findByEmail(this.email)).isEqualTo(original);
	}

	@Test
	void servesTheAttendeeWorkflowOverHttp() {
		RestClient client = RestClient.create("http://localhost:" + this.port);
		String suppliedEmail = " " + this.email.toUpperCase(Locale.ROOT).replace("@", "+news@") + " ";
		String normalizedEmail = this.email.replace("@", "+news@");
		try {
			var created = client.post().uri("/api/attendees").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("name", "Ana O'León", "email", suppliedEmail))
				.retrieve().toEntity(AttendeeResponse.class);
			assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			assertThat(created.getBody()).isNotNull();
			assertThat(created.getBody().email()).isEqualTo(normalizedEmail);
			assertThat(created.getHeaders().getLocation()).isNotNull();
			assertThat(client.get().uri(created.getHeaders().getLocation()).retrieve().body(AttendeeResponse.class))
				.isEqualTo(created.getBody());
			assertThat(client.get().uri("/api/attendees/{email}", suppliedEmail).retrieve().body(AttendeeResponse.class))
				.isEqualTo(created.getBody());

			assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> client.post().uri("/api/attendees").contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("name", "Another Ana", "email", normalizedEmail)).retrieve().toBodilessEntity())
				.satisfies(error -> {
					assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
					assertThat(error.getResponseHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
					assertThat(error.getResponseBodyAsString()).contains("An attendee with this email already exists.");
				});
			assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> client.post().uri("/api/attendees").contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("name", "Ana", "email", "invalid")).retrieve().toBodilessEntity())
				.satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
			assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> client.get().uri("/api/attendees/{email}", this.email).retrieve().toBodilessEntity())
				.satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
		}
		finally {
			this.attendees.findByEmail(normalizedEmail).ifPresent(this.attendees::delete);
		}
	}

	@Test
	void twoConcurrentTransactionsCreateOnlyOneAttendeeAndTheOtherReceives409() throws Exception {
		CyclicBarrier start = new CyclicBarrier(2);
		Callable<HttpStatusCode> create = () -> {
			start.await(10, TimeUnit.SECONDS);
			try {
				this.service.create(new CreateAttendeeRequest("Ana", this.email, null));
				return HttpStatus.CREATED;
			}
			catch (ResponseStatusException ex) {
				assertThat(ex.getReason()).isEqualTo("An attendee with this email already exists.");
				return ex.getStatusCode();
			}
		};

		// The test has no enclosing transaction: each worker commits or rolls back its own.
		try (var workers = Executors.newFixedThreadPool(2)) {
			var first = workers.submit(create);
			var second = workers.submit(create);
			assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
				.containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
		}

		assertThat(this.attendees.findAll()).filteredOn(attendee -> this.email.equals(attendee.getEmail())).hasSize(1);
	}

}
