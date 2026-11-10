package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.eventiq.OracleTestDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ OracleTestDatabase.class, AttendeeService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AttendeeServiceIT {

	@Autowired
	private AttendeeService service;

	@Autowired
	private AttendeeRepository attendees;

	private final String email = "attendee-" + UUID.randomUUID() + "@example.com";

	// Removes this test's committed row from the disposable database.
	@AfterEach
	void removeCommittedTestData() {
		this.attendees.findByEmail(this.email).ifPresent(this.attendees::delete);
	}

	// Verifies persistence and lookup of the canonical email against Oracle.
	@Test
	void commitsTheCanonicalEmailAndFindsItRegardlessOfCaseOrOuterSpaces() {
		String suppliedEmail = "  " + this.email.toUpperCase(Locale.ROOT) + "  ";
		AttendeeResponse created = this.service.create(new CreateAttendeeRequest("Ana O'León", suppliedEmail, null));

		assertThat(created.attendeeId()).isPositive();
		assertThat(created.email()).isEqualTo(this.email);
		assertThat(this.service.findByEmail(suppliedEmail)).isEqualTo(created);
		assertThat(this.attendees.findById(created.attendeeId()).orElseThrow().getEmail()).isEqualTo(this.email);
	}

	// Verifies that a rejected duplicate leaves the original attendee unchanged.
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

	// Verifies that two concurrent creates produce one row and one conflict.
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
