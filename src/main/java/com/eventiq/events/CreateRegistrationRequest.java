package com.eventiq.events;

import jakarta.validation.constraints.NotNull;

public record CreateRegistrationRequest(
		@NotNull(message = "Event id is required.") Long eventId,
		@NotNull(message = "Attendee id is required.") Long attendeeId) {

}
