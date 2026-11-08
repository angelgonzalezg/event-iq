package com.eventiq.events;

import java.time.LocalDate;

public record EventResponse(Long eventId, String name, String description, LocalDate eventDate, String location,
		Integer capacity) {

	static EventResponse from(Event event) {
		return new EventResponse(event.getEventId(), event.getName(), event.getDescription(), event.getEventDate(),
				event.getLocation(), event.getCapacity());
	}
}