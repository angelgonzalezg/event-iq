package com.eventiq.payments;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.eventiq.events.Event;
import com.eventiq.events.Registration;

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

@Entity
@Table(name = "payments")
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	private Long paymentId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registration_id", nullable = false)
	private Registration registration;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ticket_type_id", nullable = false)
	private TicketType ticketType;

	// Oracle's composite foreign keys require the registration and ticket type to share this event.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "event_id", nullable = false)
	private Event event;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	// Oracle DATE preserves seconds. These values represent UTC, without a stored time zone.
	@Column(name = "paid_at", columnDefinition = "DATE")
	private LocalDateTime paidAt;

	@Column(name = "refunded_at", columnDefinition = "DATE")
	private LocalDateTime refundedAt;

	@Convert(converter = PaymentStatusConverter.class)
	// First insertion uses Oracle's pending default; subsequent updates can change the status.
	@Generated(event = EventType.INSERT)
	@Column(nullable = false, insertable = false, length = 20)
	private PaymentStatus status;

	protected Payment() {
	}

	public Payment(Registration registration, TicketType ticketType, BigDecimal amount) {
		this.registration = registration;
		this.ticketType = ticketType;
		this.event = registration.getEvent();
		this.amount = amount;
	}

	public Long getPaymentId() {
		return this.paymentId;
	}

	public Registration getRegistration() {
		return this.registration;
	}

	public void setRegistration(Registration registration) {
		this.registration = registration;
	}

	public TicketType getTicketType() {
		return this.ticketType;
	}

	public void setTicketType(TicketType ticketType) {
		this.ticketType = ticketType;
	}

	public Event getEvent() {
		return this.event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public BigDecimal getAmount() {
		return this.amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDateTime getPaidAt() {
		return this.paidAt;
	}

	public void setPaidAt(LocalDateTime paidAt) {
		this.paidAt = paidAt;
	}

	public LocalDateTime getRefundedAt() {
		return this.refundedAt;
	}

	public void setRefundedAt(LocalDateTime refundedAt) {
		this.refundedAt = refundedAt;
	}

	public PaymentStatus getStatus() {
		return this.status;
	}

	public void setStatus(PaymentStatus status) {
		this.status = status;
	}

}
