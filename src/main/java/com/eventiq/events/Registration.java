package com.eventiq.events;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "registrations", uniqueConstraints =
		@UniqueConstraint(name = "uq_registration", columnNames = { "event_id", "attendee_id" }))
public class Registration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "registration_id")
	private Long registrationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "attendee_id", nullable = false)
	private Attendee attendee;

	// Oracle DATE preserves seconds. These values represent UTC, without a stored time zone.
	@Generated(event = EventType.INSERT)
	@Column(name = "registered_at", nullable = false, insertable = false, columnDefinition = "DATE")
	private LocalDateTime registeredAt;

	@Column(name = "cancelled_at", columnDefinition = "DATE")
	private LocalDateTime cancelledAt;

	@Convert(converter = RegistrationStatusConverter.class)
	// First insertion uses Oracle's pending default; subsequent updates can change the status.
	@Generated(event = EventType.INSERT)
	@Column(nullable = false, insertable = false, length = 20)
	private RegistrationStatus status;

	protected Registration() {
	}

	public Registration(Event event, Attendee attendee) {
		this.event = event;
		this.attendee = attendee;
	}

	public Long getRegistrationId() {
		return this.registrationId;
	}

	public Event getEvent() {
		return this.event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Attendee getAttendee() {
		return this.attendee;
	}

	public void setAttendee(Attendee attendee) {
		this.attendee = attendee;
	}

	public LocalDateTime getRegisteredAt() {
		return this.registeredAt;
	}

	public void setRegisteredAt(LocalDateTime registeredAt) {
		this.registeredAt = registeredAt;
	}

	public LocalDateTime getCancelledAt() {
		return this.cancelledAt;
	}

	public void setCancelledAt(LocalDateTime cancelledAt) {
		this.cancelledAt = cancelledAt;
	}

	public RegistrationStatus getStatus() {
		return this.status;
	}

	public void setStatus(RegistrationStatus status) {
		this.status = status;
	}

}
