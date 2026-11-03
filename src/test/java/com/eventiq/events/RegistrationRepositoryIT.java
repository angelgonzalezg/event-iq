package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.eventiq.OracleTestDatabase;

/**
 * Registrations against a real Oracle database created from {@code db/schema.sql}: the
 * defaults and constraints live in the schema, so only Oracle can test them. The reference for
 * repository tests; see {@code docs/testing.md}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleTestDatabase.class)
class RegistrationRepositoryIT {

	@Autowired
	private EventRepository events;

	@Autowired
	private AttendeeRepository attendees;

	@Autowired
	private RegistrationRepository registrations;

	private Event event;

	private Attendee attendee;

	@BeforeEach
	void saveAnEventAndAnAttendee() {
		this.event = this.events.save(new Event("Java Day", null, LocalDate.now().plusDays(7), "Online", 50));
		this.attendee = this.attendees.save(new Attendee("Ana Lopez", "ana.lopez@example.com", null));
	}

	@Test
	void newRegistrationsArePendingAndStampedByOracle() {
		Registration registration = this.registrations.saveAndFlush(new Registration(this.event, this.attendee));

		assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.PENDING);
		assertThat(registration.getRegisteredAt()).isNotNull();
	}

	@Test
	void anAttendeeRegistersOnlyOncePerEvent() {
		this.registrations.saveAndFlush(new Registration(this.event, this.attendee));

		assertThatThrownBy(() -> this.registrations.saveAndFlush(new Registration(this.event, this.attendee)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void aCancelledRegistrationNeedsACancellationDate() {
		Registration registration = this.registrations.saveAndFlush(new Registration(this.event, this.attendee));
		registration.setStatus(RegistrationStatus.CANCELLED);

		assertThatThrownBy(() -> this.registrations.saveAndFlush(registration))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
