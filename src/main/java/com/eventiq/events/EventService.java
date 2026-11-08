package com.eventiq.events;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {

	private final EventRepository eventRepository;

	private final RegistrationRepository registrationRepository;

	public EventService(EventRepository eventRepository, RegistrationRepository registrationRepository) {
		this.eventRepository = eventRepository;
		this.registrationRepository = registrationRepository;
	}

	@Transactional(readOnly = true)
	public List<EventResponse> findAll() {
		return this.eventRepository.findAll(Sort.by("eventDate", "eventId")).stream().map(EventResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public EventResponse findById(Long id) {
		return EventResponse.from(getEvent(id));
	}

	@Transactional
	public EventResponse create(EventRequest request) {
		Event event = new Event(request.getName(), request.getDescription(), request.getEventDate(),
				request.getLocation(), request.getCapacity());
		return EventResponse.from(this.eventRepository.save(event));
	}

	@Transactional
	public EventResponse update(Long id, EventRequest request) {
		Event event = getEvent(id);
		event.setName(request.getName());
		event.setDescription(request.getDescription());
		event.setEventDate(request.getEventDate());
		event.setLocation(request.getLocation());
		event.setCapacity(request.getCapacity());
		return EventResponse.from(event);
	}

	@Transactional
	public void delete(Long id) {
		Event event = getEvent(id);
		if (this.registrationRepository.existsByEventEventId(id)) {
			throw new EventHasRegistrationsException();
		}
		this.eventRepository.delete(event);
		this.eventRepository.flush();
	}

	private Event getEvent(Long id) {
		return this.eventRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));
	}
}