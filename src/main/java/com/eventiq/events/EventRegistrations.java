package com.eventiq.events;

import java.time.LocalDateTime;
import java.util.List;

public record EventRegistrations(EventResponse event, List<Row> attendees, long remaining) {

	public record Row(Long registrationId, String attendeeName, String attendeeEmail, String status,
			LocalDateTime registeredAt) {
	}

	public boolean full() {
		return this.remaining <= 0;
	}

}
