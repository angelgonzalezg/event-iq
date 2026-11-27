package com.eventiq.events;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

	private final EventRepository events;

	private final AttendeeRepository attendees;

	private final RegistrationRepository registrations;

	public RegistrationService(EventRepository events, AttendeeRepository attendees,
			RegistrationRepository registrations) {
		this.events = events;
		this.attendees = attendees;
		this.registrations = registrations;
	}

	@Transactional
	public RegistrationResponse register(CreateRegistrationRequest request) {
		Event event = this.events.findWithLockByEventId(request.eventId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));
		Attendee attendee = this.attendees.findById(request.attendeeId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found."));

		Registration existing = this.registrations
			.findByEventEventIdAndAttendeeAttendeeId(event.getEventId(), attendee.getAttendeeId())
			.orElse(null);
		if (existing != null && existing.getStatus() != RegistrationStatus.CANCELLED) {
			throw new AlreadyRegisteredException();
		}
		if (this.registrations.countByEventEventIdAndStatusNot(event.getEventId(),
				RegistrationStatus.CANCELLED) >= event.getCapacity()) {
			throw new EventFullException();
		}

		try {
			if (existing == null) {
				return RegistrationResponse.from(this.registrations.saveAndFlush(new Registration(event, attendee)));
			}
			existing.setStatus(RegistrationStatus.PENDING);
			return RegistrationResponse.from(this.registrations.saveAndFlush(existing));
		}
		catch (DataIntegrityViolationException ex) {
			throw new AlreadyRegisteredException();
		}
	}

	@Transactional
	public RegistrationResponse cancel(Long registrationId) {
		Registration registration = this.registrations.findById(registrationId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found."));
		if (registration.getStatus() != RegistrationStatus.CANCELLED) {
			registration.setStatus(RegistrationStatus.CANCELLED);
			registration.setCancelledAt(LocalDateTime.now(ZoneOffset.UTC));
			this.registrations.saveAndFlush(registration);
		}
		return RegistrationResponse.from(registration);
	}

	@Transactional(readOnly = true)
	public EventRegistrations findByEvent(Long eventId) {
		Event event = this.events.findById(eventId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));
		List<EventRegistrations.Row> rows = this.registrations
			.findByEventEventIdAndStatusNotOrderByRegisteredAtAscRegistrationIdAsc(eventId,
					RegistrationStatus.CANCELLED)
			.stream()
			.map(r -> new EventRegistrations.Row(r.getRegistrationId(), r.getAttendee().getName(),
					r.getAttendee().getEmail(), r.getStatus().name().toLowerCase(Locale.ROOT), r.getRegisteredAt()))
			.toList();
		long remaining = Math.max(0, event.getCapacity() - rows.size());
		return new EventRegistrations(EventResponse.from(event), rows, remaining);
	}

}
