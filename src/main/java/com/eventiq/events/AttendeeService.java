package com.eventiq.events;

import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttendeeService {

	private final AttendeeRepository attendeeRepository;

	public AttendeeService(AttendeeRepository attendeeRepository) {
		this.attendeeRepository = attendeeRepository;
	}

	@Transactional
	public AttendeeResponse create(CreateAttendeeRequest request) {
		Attendee attendee = new Attendee(request.name(), request.email(), request.phone());
		try {
			// Flush inside the transaction so concurrent duplicates receive the same 409.
			return AttendeeResponse.from(this.attendeeRepository.saveAndFlush(attendee));
		}
		catch (DataIntegrityViolationException ex) {
			if (isDuplicateEmail(ex)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "An attendee with this email already exists.", ex);
			}
			throw ex;
		}
	}

	@Transactional(readOnly = true)
	public AttendeeResponse findByEmail(String email) {
		return this.attendeeRepository.findByEmail(AttendeeEmail.normalize(email))
			.map(AttendeeResponse::from)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found."));
	}

	// Identifies the email unique constraint without masking other database errors.
	private static boolean isDuplicateEmail(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
				String constraintName = violation.getConstraintName().replace("\"", "").toUpperCase(Locale.ROOT);
				if (constraintName.equals("UQ_ATTENDEES_EMAIL") || constraintName.endsWith(".UQ_ATTENDEES_EMAIL")) {
					return true;
				}
			}
		}
		return false;
	}

}
