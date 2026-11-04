package com.eventiq.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.eventiq.OracleTestDatabase;
import com.eventiq.events.Attendee;
import com.eventiq.events.Event;
import com.eventiq.events.Registration;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleTestDatabase.class)
class PaymentRepositoryIT {

	@Autowired
	private PaymentRepository payments;

	@Autowired
	private TestEntityManager entityManager;

	private Registration registration;

	private TicketType ticketType;

	@BeforeEach
	void saveARegistrationAndTicketType() {
		Event event = this.entityManager.persist(new Event("Java Day", null, LocalDate.of(2026, 11, 10), "Online", 50));
		Attendee attendee = this.entityManager.persist(new Attendee("Ana Lopez", "ana.lopez@example.com", null));
		this.registration = this.entityManager.persist(new Registration(event, attendee));
		this.ticketType = this.entityManager.persist(new TicketType(event, "General", new BigDecimal("125.75")));
	}

	@Test
	void newPaymentsArePendingWithNoPaymentDates() {
		Payment payment = this.payments.saveAndFlush(new Payment(this.registration, this.ticketType, new BigDecimal("125.75")));

		assertThat(payment.getPaymentId()).isNotNull();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
		this.entityManager.clear();

		Payment stored = this.payments.findById(payment.getPaymentId()).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(PaymentStatus.PENDING);
		assertThat(stored.getPaidAt()).isNull();
		assertThat(stored.getRefundedAt()).isNull();
	}

	@Test
	void storesAPaidPaymentWithItsAmountDateAndRelations() {
		Payment payment = this.payments.saveAndFlush(new Payment(this.registration, this.ticketType, new BigDecimal("125.75")));
		LocalDateTime paidAt = LocalDateTime.of(2026, 11, 3, 14, 25, 36);
		payment.setStatus(PaymentStatus.PAID);
		payment.setPaidAt(paidAt);
		this.payments.saveAndFlush(payment);
		this.entityManager.clear();

		Payment stored = this.payments.findById(payment.getPaymentId()).orElseThrow();
		assertThat(stored.getStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(stored.getAmount()).isEqualByComparingTo("125.75");
		assertThat(stored.getPaidAt()).isEqualTo(paidAt);
		assertThat(stored.getRefundedAt()).isNull();
		assertThat(Hibernate.isInitialized(stored.getRegistration())).isFalse();
		assertThat(Hibernate.isInitialized(stored.getTicketType())).isFalse();
		assertThat(Hibernate.isInitialized(stored.getEvent())).isFalse();
		assertThat(stored.getRegistration().getRegistrationId()).isEqualTo(this.registration.getRegistrationId());
		assertThat(stored.getTicketType().getTicketTypeId()).isEqualTo(this.ticketType.getTicketTypeId());
		assertThat(stored.getEvent().getEventId()).isEqualTo(this.registration.getEvent().getEventId());
	}

	@Test
	void rejectsAPaidPaymentWithoutAPaymentDate() {
		Payment payment = this.payments.saveAndFlush(new Payment(this.registration, this.ticketType, BigDecimal.ZERO));
		payment.setStatus(PaymentStatus.PAID);

		assertThatThrownBy(() -> this.payments.saveAndFlush(payment))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsATicketTypeFromAnotherEvent() {
		Event otherEvent = this.entityManager.persist(new Event("Python Day", null, LocalDate.of(2026, 11, 11), "Online", 50));
		TicketType otherTicketType = this.entityManager.persist(new TicketType(otherEvent, "General", BigDecimal.ZERO));

		assertThatThrownBy(() -> this.payments.saveAndFlush(new Payment(this.registration, otherTicketType, BigDecimal.ZERO)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
