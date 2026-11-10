package com.eventiq.events;

public record AttendeeResponse(Long attendeeId, String name, String email, String phone) {

	// Copies the attendee's stored fields into the API response.
	static AttendeeResponse from(Attendee attendee) {
		return new AttendeeResponse(attendee.getAttendeeId(), attendee.getName(), attendee.getEmail(), attendee.getPhone());
	}

}
