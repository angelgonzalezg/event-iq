package com.eventiq.events;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendees")
public class AttendeeApiController {

	private final AttendeeService attendees;

	public AttendeeApiController(AttendeeService attendees) {
		this.attendees = attendees;
	}

	@PostMapping
	public ResponseEntity<AttendeeResponse> create(@Valid @RequestBody CreateAttendeeRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(this.attendees.create(request));
	}

	@GetMapping("/{email}")
	public AttendeeResponse findByEmail(@PathVariable String email) {
		return this.attendees.findByEmail(email);
	}

	@GetMapping(params = "email")
	public AttendeeResponse findByEmailParameter(@RequestParam("email") String email) {
		return this.attendees.findByEmail(email);
	}

}
