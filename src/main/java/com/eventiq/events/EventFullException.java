package com.eventiq.events;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EventFullException extends ResponseStatusException {

	public EventFullException() {
		super(HttpStatus.CONFLICT, "The event is full.");
	}

}
