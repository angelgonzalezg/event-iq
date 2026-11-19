package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AttendeeServiceTests {

	@Mock
	private AttendeeRepository attendees;

	@InjectMocks
	private AttendeeService service;

	@Test
	void createsAnAttendeeWithANormalizedEmailAndReturnsItsSavedData() {
		CreateAttendeeRequest request = new CreateAttendeeRequest("Ana O'León", "  ANA.LOPEZ+news@Example.COM  ", "+52 81 1234 5678");
		Attendee saved = attendee(7L, request.name(), "ana.lopez+news@example.com", request.phone());
		when(this.attendees.saveAndFlush(any(Attendee.class))).thenReturn(saved);

		AttendeeResponse response = this.service.create(request);

		ArgumentCaptor<Attendee> submitted = ArgumentCaptor.forClass(Attendee.class);
		verify(this.attendees).saveAndFlush(submitted.capture());
		assertThat(submitted.getValue().getName()).isEqualTo("Ana O'León");
		assertThat(submitted.getValue().getEmail()).isEqualTo("ana.lopez+news@example.com");
		assertThat(submitted.getValue().getPhone()).isEqualTo(request.phone());
		assertThat(response).isEqualTo(new AttendeeResponse(7L, request.name(), saved.getEmail(), request.phone()));
	}

	@Test
	void looksUpTheCanonicalEmailAndReturnsTheAttendee() {
		when(this.attendees.findByEmail("ana.lopez+news@example.com"))
			.thenReturn(Optional.of(attendee(7L, "Ana Lopez", "ana.lopez+news@example.com", null)));

		assertThat(this.service.findByEmail("  ANA.LOPEZ+news@Example.COM  "))
			.isEqualTo(new AttendeeResponse(7L, "Ana Lopez", "ana.lopez+news@example.com", null));
	}

	@Test
	void answers404WhenTheEmailIsNotRegistered() {
		when(this.attendees.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResponseStatusException.class)
			.isThrownBy(() -> this.service.findByEmail("missing@example.com"))
			.satisfies(error -> {
				assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(error.getReason()).isEqualTo("Attendee not found.");
			});
	}

	@ParameterizedTest
	@ValueSource(strings = { "UQ_ATTENDEES_EMAIL", "APP_USER.UQ_ATTENDEES_EMAIL", "\"APP_USER\".\"UQ_ATTENDEES_EMAIL\"" })
	void translatesTheEmailConstraintToAClearConflict(String constraint) {
		DataIntegrityViolationException duplicate = integrityViolation(constraint);
		when(this.attendees.saveAndFlush(any(Attendee.class))).thenThrow(duplicate);

		assertThatExceptionOfType(ResponseStatusException.class)
			.isThrownBy(() -> this.service.create(new CreateAttendeeRequest("Ana", "ana@example.com", null)))
			.satisfies(error -> {
				assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(error.getReason()).isEqualTo("An attendee with this email already exists.");
				assertThat(error.getCause()).isSameAs(duplicate);
			});
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "CK_ATTENDEES_EMAIL", "PK_ATTENDEES", "UQ_OTHER_EMAIL" })
	void doesNotMisreportOtherDatabaseConstraintsAsDuplicateEmails(String constraint) {
		DataIntegrityViolationException failure = integrityViolation(constraint);
		when(this.attendees.saveAndFlush(any(Attendee.class))).thenThrow(failure);

		assertThatThrownBy(() -> this.service.create(new CreateAttendeeRequest("Ana", "ana@example.com", null)))
			.isSameAs(failure);
	}

	@Test
	void preservesAnIntegrityFailureWithoutAHibernateConstraint() {
		DataIntegrityViolationException failure = new DataIntegrityViolationException("Unknown integrity failure");
		when(this.attendees.saveAndFlush(any(Attendee.class))).thenThrow(failure);

		assertThatThrownBy(() -> this.service.create(new CreateAttendeeRequest("Ana", "ana@example.com", null)))
			.isSameAs(failure);
	}

	private static DataIntegrityViolationException integrityViolation(String constraint) {
		return new DataIntegrityViolationException("Database rejected the insert",
				new ConstraintViolationException("Constraint violation", new SQLException("Constraint violation"), constraint));
	}

	private static Attendee attendee(Long id, String name, String email, String phone) {
		Attendee attendee = new Attendee(name, email, phone);
		ReflectionTestUtils.setField(attendee, "attendeeId", id);
		return attendee;
	}

}
