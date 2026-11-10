package com.eventiq.events;

import java.util.Locale;

/** The shared email representation required by Oracle for writes and lookups. */
final class AttendeeEmail {

	// Prevents instantiation of this email utility.
	private AttendeeEmail() {
	}

	// Trims outer spaces and lowercases the email for writes and lookups.
	static String normalize(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

}
