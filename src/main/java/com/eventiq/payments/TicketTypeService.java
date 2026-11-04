package com.eventiq.payments;

import java.util.List;

import com.eventiq.events.Event;
import com.eventiq.events.EventRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketTypeService {

	private final TicketTypeRepository ticketTypeRepository;

	private final EventRepository eventRepository;

	public TicketTypeService(TicketTypeRepository ticketTypeRepository, EventRepository eventRepository) {
		this.ticketTypeRepository = ticketTypeRepository;
		this.eventRepository = eventRepository;
	}

	@Transactional
	public TicketTypeResponse create(CreateTicketTypeRequest request) {
		Event event = this.eventRepository.findById(request.eventId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));
		TicketType ticketType = new TicketType(event, request.name(), request.price());
		return TicketTypeResponse.from(this.ticketTypeRepository.save(ticketType));
	}

	@Transactional(readOnly = true)
	public List<TicketTypeResponse> findAll() {
		return this.ticketTypeRepository.findAll().stream().map(TicketTypeResponse::from).toList();
	}

}
