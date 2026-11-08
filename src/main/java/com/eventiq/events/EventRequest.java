package com.eventiq.events;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;


public class EventRequest {

	@NotBlank(message = "{events.form.name.required}")
	@Size(max = 150, message = "{events.form.name.size}")
	private String name;

	@Size(max = 2000, message = "{events.form.description.size}")
	private String description;

	@NotNull(message = "{events.form.date.required}")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate eventDate;

	@Size(max = 150, message = "{events.form.location.size}")
	private String location;

	@NotNull(message = "{events.form.capacity.required}")
	@Positive(message = "{events.form.capacity.positive}")
	private Integer capacity;

	public EventRequest() {
	}


	static EventRequest from(EventResponse event) {
		EventRequest request = new EventRequest();
		request.setName(event.name());
		request.setDescription(event.description());
		request.setEventDate(event.eventDate());
		request.setLocation(event.location());
		request.setCapacity(event.capacity());
		return request;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getEventDate() {
		return this.eventDate;
	}

	public void setEventDate(LocalDate eventDate) {
		this.eventDate = eventDate;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Integer getCapacity() {
		return this.capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}
}
