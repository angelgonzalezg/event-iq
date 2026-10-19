package com.eventiq.events;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "event_id")
	private Long eventId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "event_date", nullable = false)
	private LocalDate eventDate;

	@Column(length = 150)
	private String location;

	@Column(nullable = false)
	private Integer capacity;

	protected Event() {
	}

	public Event(String name, String description, LocalDate eventDate, String location, Integer capacity) {
		this.name = name;
		this.description = description;
		this.eventDate = eventDate;
		this.location = location;
		this.capacity = capacity;
	}

	public Long getEventId() {
		return this.eventId;
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
