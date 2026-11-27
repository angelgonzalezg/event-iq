package com.eventiq.events;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AlreadyRegisteredException extends ResponseStatusException {

	public AlreadyRegisteredException() {
		super(HttpStatus.CONFLICT, "The attendee is already registered for this event.");
	}

}
