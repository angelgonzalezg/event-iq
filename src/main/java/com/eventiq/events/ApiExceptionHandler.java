package com.eventiq.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(annotations = RestController.class)
class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidFields(MethodArgumentNotValidException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"One or more fields are invalid.");
		List<Map<String, String>> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> Map.of("field", error.getField(), "message",
					Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value.")))
			.toList();
		problem.setProperty("errors", errors);
		return problem;
	}

	/** The body is missing, is not JSON, or has a value of the wrong type (such as a bad date). */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body is missing or malformed.");
	}

	/** Errors the services raise on purpose, such as 404 Not Found or 409 Conflict. */
	@ExceptionHandler(ResponseStatusException.class)
	ProblemDetail handleResponseStatus(ResponseStatusException ex) {
		return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
	}

	/** The database refused the change, for example a foreign key from another table. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The operation conflicts with existing data, such as records that reference this one.");
	}
}
