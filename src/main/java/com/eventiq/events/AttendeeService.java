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

	private final AttendeeRepository attendees;

	public AttendeeService(AttendeeRepository attendees) {
		this.attendees = attendees;
	}

	@Transactional
	public AttendeeResponse create(CreateAttendeeRequest request) {
		Attendee attendee = new Attendee(request.name(), request.email(), request.phone());
		try {
			// Flush inside the transaction so concurrent duplicates receive the same 409.
			return AttendeeResponse.from(this.attendees.saveAndFlush(attendee));
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
		return this.attendees.findByEmail(AttendeeEmail.normalize(email))
			.map(AttendeeResponse::from)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found."));
	}

	private static boolean isDuplicateEmail(Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
				String name = violation.getConstraintName().replace("\"", "").toUpperCase(Locale.ROOT);
				if (name.equals("UQ_ATTENDEES_EMAIL") || name.endsWith(".UQ_ATTENDEES_EMAIL")) {
					return true;
				}
			}
		}
		return false;
	}

}
