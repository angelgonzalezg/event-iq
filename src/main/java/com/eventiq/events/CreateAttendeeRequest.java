package com.eventiq.events;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAttendeeRequest(
		@NotBlank(message = "Name is required.")
		@Size(max = 100, message = "Name must be at most 100 characters.") String name,
		@NotBlank(message = "Email is required.")
		@Email(message = "Email must be a valid email address.")
		@Size(max = 100, message = "Email must be at most 100 characters.") String email,
		@Size(max = 20, message = "Phone must be at most 20 characters.") String phone) {

	public CreateAttendeeRequest {
		email = AttendeeEmail.normalize(email);
	}

}
