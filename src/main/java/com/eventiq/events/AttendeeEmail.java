package com.eventiq.events;

import java.util.Locale;

/** The shared email representation required by Oracle for writes and lookups. */
final class AttendeeEmail {

	private AttendeeEmail() {
	}

	static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

}
