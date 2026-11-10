package com.eventiq.events;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendees")
public class AttendeeApiController {

	private final AttendeeService attendeeService;

	// Receives the attendee service through constructor injection.
	public AttendeeApiController(AttendeeService attendeeService) {
		this.attendeeService = attendeeService;
	}

	// Validates the request and returns the created attendee with HTTP 201.
	@PostMapping
	public ResponseEntity<AttendeeResponse> create(@Valid @RequestBody CreateAttendeeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(this.attendeeService.create(request));
	}

	// Looks up an attendee using the email query parameter.
	@GetMapping(params = "email")
	public AttendeeResponse findByEmail(@RequestParam("email") String email) {
		return this.attendeeService.findByEmail(email);
	}

}
