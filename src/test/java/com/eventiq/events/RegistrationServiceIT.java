package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eventiq.OracleTestDatabase;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ OracleTestDatabase.class, RegistrationService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RegistrationServiceIT {

	@Autowired
	private RegistrationService service;

	@Autowired
	private EventRepository events;

	@Autowired
	private AttendeeRepository attendees;

	@Autowired
	private RegistrationRepository registrations;

	private Event event;

	private final List<Attendee> people = new ArrayList<>();

	@BeforeEach
	void createAnEventWithOneSeat() {
		this.event = this.events.save(new Event("Last seat", null, LocalDate.now().plusDays(7), "Online", 1));
		for (int i = 0; i < 2; i++) {
			this.people.add(this.attendees.save(new Attendee("Person " + i, "p" + i + "-" + UUID.randomUUID() + "@example.com", null)));
		}
	}

	@AfterEach
	void removeCommittedTestData() {
		this.registrations.deleteAll(this.registrations.findAll().stream()
			.filter(r -> r.getEvent().getEventId().equals(this.event.getEventId()))
			.toList());
		this.events.deleteById(this.event.getEventId());
		this.attendees.deleteAll(this.people);
	}

	private CreateRegistrationRequest request(int person) {
		return new CreateRegistrationRequest(this.event.getEventId(), this.people.get(person).getAttendeeId());
	}

	@Test
	void theSecondAttendeeGets409WhenTheEventIsFull() {
		assertThat(this.service.register(request(0)).status()).isEqualTo("pending");

		assertThatExceptionOfType(EventFullException.class).isThrownBy(() -> this.service.register(request(1)));
	}

	@Test
	void registeringTwiceGets409() {
		this.service.register(request(0));

		assertThatExceptionOfType(AlreadyRegisteredException.class).isThrownBy(() -> this.service.register(request(0)));
	}

	@Test
	void cancellingFreesTheSeatAndTheSameAttendeeCanComeBack() {
		RegistrationResponse first = this.service.register(request(0));
		this.service.cancel(first.registrationId());
		assertThat(this.service.findByEvent(this.event.getEventId()).remaining()).isEqualTo(1);

		RegistrationResponse again = this.service.register(request(0));

		assertThat(again.registrationId()).isEqualTo(first.registrationId());
		assertThat(again.status()).isEqualTo("pending");
		assertThat(this.service.findByEvent(this.event.getEventId()).remaining()).isZero();
	}

	@Test
	void twoSimultaneousRequestsForTheLastSeatLetOnlyOneSucceed() throws Exception {
		CyclicBarrier start = new CyclicBarrier(2);
		List<Callable<String>> workers = List.of(1, 0).stream().<Callable<String>>map(person -> () -> {
			start.await(10, TimeUnit.SECONDS);
			try {
				this.service.register(request(person));
				return "created";
			}
			catch (ResponseStatusException ex) {
				return ex.getStatusCode().value() + "";
			}
		}).toList();

		try (var pool = Executors.newFixedThreadPool(2)) {
			var first = pool.submit(workers.get(0));
			var second = pool.submit(workers.get(1));
			assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
				.containsExactlyInAnyOrder("created", "409");
		}

		assertThat(this.registrations.countByEventEventIdAndStatusNot(this.event.getEventId(),
				RegistrationStatus.CANCELLED)).isEqualTo(1);
	}

}
