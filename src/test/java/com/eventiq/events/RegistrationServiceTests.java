package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTests {

	@Mock
	private EventRepository events;

	@Mock
	private AttendeeRepository attendees;

	@Mock
	private RegistrationRepository registrations;

	private RegistrationService service;

	private final Event event = new Event("Java Day", null, LocalDate.now().plusDays(7), "Online", 2);

	private final Attendee attendee = new Attendee("Ana Lopez", "ana@example.com", null);

	private final CreateRegistrationRequest request = new CreateRegistrationRequest(1L, 2L);

	@BeforeEach
	void setUp() {
		this.service = new RegistrationService(this.events, this.attendees, this.registrations);
	}

	private void eventAndAttendeeExist() {
		when(this.events.findWithLockByEventId(1L)).thenReturn(Optional.of(this.event));
		when(this.attendees.findById(2L)).thenReturn(Optional.of(this.attendee));
		when(this.registrations.findByEventEventIdAndAttendeeAttendeeId(any(), any())).thenReturn(Optional.empty());
	}

	@Test
	void registersAnAttendeeAsPendingWhenThereIsRoom() {
		eventAndAttendeeExist();
		when(this.registrations.countByEventEventIdAndStatusNot(any(), any())).thenReturn(1L);
		when(this.registrations.saveAndFlush(any(Registration.class))).thenAnswer(call -> {
			Registration saved = call.getArgument(0);
			saved.setStatus(RegistrationStatus.PENDING);
			return saved;
		});

		RegistrationResponse response = this.service.register(this.request);

		assertThat(response.status()).isEqualTo("pending");
	}

	@Test
	void rejectsTheRegistrationWhenTheEventIsFull() {
		eventAndAttendeeExist();
		when(this.registrations.countByEventEventIdAndStatusNot(any(), any())).thenReturn(2L);

		assertThatExceptionOfType(EventFullException.class).isThrownBy(() -> this.service.register(this.request))
			.satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(this.registrations, never()).saveAndFlush(any());
	}

	@Test
	void rejectsAnAttendeeWhoHoldsASeatAlready() {
		eventAndAttendeeExist();
		Registration active = new Registration(this.event, this.attendee);
		active.setStatus(RegistrationStatus.CONFIRMED);
		when(this.registrations.findByEventEventIdAndAttendeeAttendeeId(any(), any())).thenReturn(Optional.of(active));

		assertThatExceptionOfType(AlreadyRegisteredException.class)
			.isThrownBy(() -> this.service.register(this.request))
			.satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(this.registrations, never()).saveAndFlush(any());
	}

	@Test
	void reactivatesACancelledRegistrationWhenThereIsRoom() {
		eventAndAttendeeExist();
		Registration cancelled = new Registration(this.event, this.attendee);
		cancelled.setStatus(RegistrationStatus.CANCELLED);
		when(this.registrations.findByEventEventIdAndAttendeeAttendeeId(any(), any())).thenReturn(Optional.of(cancelled));
		when(this.registrations.countByEventEventIdAndStatusNot(any(), any())).thenReturn(0L);
		when(this.registrations.saveAndFlush(cancelled)).thenReturn(cancelled);

		assertThat(this.service.register(this.request).status()).isEqualTo("pending");
	}

	@Test
	void aCancelledAttendeeCannotTakeASeatInAFullEvent() {
		eventAndAttendeeExist();
		Registration cancelled = new Registration(this.event, this.attendee);
		cancelled.setStatus(RegistrationStatus.CANCELLED);
		when(this.registrations.findByEventEventIdAndAttendeeAttendeeId(any(), any())).thenReturn(Optional.of(cancelled));
		when(this.registrations.countByEventEventIdAndStatusNot(any(), any())).thenReturn(2L);

		assertThatExceptionOfType(EventFullException.class).isThrownBy(() -> this.service.register(this.request));
		assertThat(cancelled.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
	}

	@Test
	void mapsTheUniqueConstraintToAlreadyRegistered() {
		eventAndAttendeeExist();
		when(this.registrations.countByEventEventIdAndStatusNot(any(), any())).thenReturn(0L);
		when(this.registrations.saveAndFlush(any(Registration.class))).thenThrow(new DataIntegrityViolationException("uq_registration"));

		assertThatExceptionOfType(AlreadyRegisteredException.class).isThrownBy(() -> this.service.register(this.request));
	}

	@Test
	void answers404WhenTheEventOrTheAttendeeDoNotExist() {
		when(this.events.findWithLockByEventId(1L)).thenReturn(Optional.empty());
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> this.service.register(this.request))
			.satisfies(error -> assertThat(error.getReason()).isEqualTo("Event not found."));

		when(this.events.findWithLockByEventId(1L)).thenReturn(Optional.of(this.event));
		when(this.attendees.findById(2L)).thenReturn(Optional.empty());
		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> this.service.register(this.request))
			.satisfies(error -> assertThat(error.getReason()).isEqualTo("Attendee not found."));
	}

	@Test
	void cancellingSetsTheStatusAndTheCancellationDate() {
		Registration registration = new Registration(this.event, this.attendee);
		registration.setStatus(RegistrationStatus.PENDING);
		when(this.registrations.findById(5L)).thenReturn(Optional.of(registration));

		this.service.cancel(5L);

		assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
		assertThat(registration.getCancelledAt()).isNotNull();
		verify(this.registrations).saveAndFlush(registration);
	}

	@Test
	void cancellingAnAlreadyCancelledRegistrationChangesNothing() {
		Registration registration = new Registration(this.event, this.attendee);
		registration.setStatus(RegistrationStatus.CANCELLED);
		when(this.registrations.findById(5L)).thenReturn(Optional.of(registration));

		this.service.cancel(5L);

		assertThat(registration.getCancelledAt()).isNull();
		verify(this.registrations, never()).saveAndFlush(any());
	}

	@Test
	void cancellingAnUnknownRegistrationAnswers404() {
		when(this.registrations.findById(5L)).thenReturn(Optional.empty());

		assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> this.service.cancel(5L))
			.satisfies(error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

}
