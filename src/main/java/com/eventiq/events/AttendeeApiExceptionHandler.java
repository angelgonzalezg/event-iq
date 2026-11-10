package com.eventiq.events;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = AttendeeApiController.class)
class AttendeeApiExceptionHandler {

	// Builds an HTTP 400 response with field names and validation messages.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidFields(MethodArgumentNotValidException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"One or more attendee fields are invalid.");
		problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
			.map(error -> Map.of("field", error.getField(), "message",
					Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value.")))
			.toList());
		return problem;
	}

	// Reports a missing or malformed JSON body as HTTP 400.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body is missing or malformed.");
	}

	// Returns the status and readable message raised by the service.
	@ExceptionHandler(ResponseStatusException.class)
	ProblemDetail handleResponseStatus(ResponseStatusException ex) {
		return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
	}

}
