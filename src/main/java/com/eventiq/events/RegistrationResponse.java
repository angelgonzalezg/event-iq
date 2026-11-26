package com.eventiq.events;

import java.time.LocalDateTime;
import java.util.Locale;

public record RegistrationResponse(Long registrationId, Long eventId, Long attendeeId, String status,
		LocalDateTime registeredAt) {

	static RegistrationResponse from(Registration registration) {
		return new RegistrationResponse(registration.getRegistrationId(), registration.getEvent().getEventId(),
				registration.getAttendee().getAttendeeId(), registration.getStatus().name().toLowerCase(Locale.ROOT),
				registration.getRegisteredAt());
	}

}
