package com.eventiq.events;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api/events")
public class EventApiController {

	private final EventService eventService;

	public EventApiController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping
	public List<EventResponse> findAll() {
		return this.eventService.findAll();
	}

	@GetMapping("/{id}")
	public EventResponse findById(@PathVariable Long id) {
		return this.eventService.findById(id);
	}

	@PostMapping
	public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
		EventResponse created = this.eventService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.eventId())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
		return this.eventService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		this.eventService.delete(id);
	}
}
