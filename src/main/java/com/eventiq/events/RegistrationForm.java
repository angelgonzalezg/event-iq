package com.eventiq.events;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegistrationForm {

	@NotNull(message = "{registrations.form.event.required}")
	private Long eventId;

	@NotBlank(message = "{registrations.form.email.required}")
	@Email(message = "{registrations.form.email.invalid}")
	private String email;

	public Long getEventId() {
		return this.eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
