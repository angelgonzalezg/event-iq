package com.eventiq.payments;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ticket-types")
public class TicketTypeApiController {

	private final TicketTypeService ticketTypeService;

	public TicketTypeApiController(TicketTypeService ticketTypeService) {
		this.ticketTypeService = ticketTypeService;
	}

	@PostMapping
	public ResponseEntity<?> create(@Valid @RequestBody CreateTicketTypeRequest request, BindingResult result) {
		if (result.hasErrors()) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
					"One or more ticket type fields are invalid.");
			problem.setProperty("errors", result.getFieldErrors().stream()
				.map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
				.toList());
			return ResponseEntity.badRequest().body(problem);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(this.ticketTypeService.create(request));
	}

	@GetMapping
	public List<TicketTypeResponse> findAll() {
		return this.ticketTypeService.findAll();
	}

}
