package com.eventiq.events;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EventHasRegistrationsException extends ResponseStatusException {

	public EventHasRegistrationsException() {
		super(HttpStatus.CONFLICT, "The event has registrations and cannot be deleted.");
	}
}