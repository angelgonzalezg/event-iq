package com.eventiq.events;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/attendees")
public class AttendeeApiController {

	private final AttendeeService attendees;

	public AttendeeApiController(AttendeeService attendees) {
		this.attendees = attendees;
	}

	@PostMapping
	public ResponseEntity<AttendeeResponse> create(@Valid @RequestBody CreateAttendeeRequest request) {
		AttendeeResponse created = this.attendees.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.pathSegment("{email}")
			.encode()
			.buildAndExpand(created.email())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@GetMapping("/{email}")
	public AttendeeResponse findByEmail(@PathVariable String email) {
		return this.attendees.findByEmail(email);
	}

}
