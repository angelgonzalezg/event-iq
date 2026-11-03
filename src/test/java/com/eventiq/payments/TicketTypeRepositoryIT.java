package com.eventiq.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import com.eventiq.events.Event;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleTestDatabase.class)
class TicketTypeRepositoryIT {

	@Autowired
	private TicketTypeRepository ticketTypes;

	@Autowired
	private TestEntityManager entityManager;

	private Event event;

	@BeforeEach
	void saveAnEvent() {
		this.event = this.entityManager.persist(new Event("Java Day", null, LocalDate.of(2026, 11, 10), "Online", 50));
	}

	@Test
	void storesTheNamePriceAndEvent() {
		TicketType ticketType = this.ticketTypes.saveAndFlush(new TicketType(this.event, "General", new BigDecimal("125.75")));
		assertThat(ticketType.getTicketTypeId()).isNotNull();
		this.entityManager.clear();

		TicketType stored = this.ticketTypes.findById(ticketType.getTicketTypeId()).orElseThrow();
		assertThat(stored.getName()).isEqualTo("General");
		assertThat(stored.getPrice()).isEqualByComparingTo("125.75");
		assertThat(Hibernate.isInitialized(stored.getEvent())).isFalse();
		assertThat(stored.getEvent().getEventId()).isEqualTo(this.event.getEventId());
	}

	@Test
	void allowsFreeTickets() {
		TicketType ticketType = this.ticketTypes.saveAndFlush(new TicketType(this.event, "Free", BigDecimal.ZERO));
		this.entityManager.clear();

		assertThat(this.ticketTypes.findById(ticketType.getTicketTypeId()).orElseThrow().getPrice()).isZero();
	}

	@Test
	void rejectsNegativePrices() {
		assertThatThrownBy(() -> this.ticketTypes.saveAndFlush(new TicketType(this.event, "General", new BigDecimal("-0.01"))))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
