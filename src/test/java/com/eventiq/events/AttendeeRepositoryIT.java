package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventiq.OracleTestDatabase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleTestDatabase.class)
class AttendeeRepositoryIT {

	@Autowired
	private AttendeeRepository attendees;

	// Verifies that Oracle enforces email uniqueness regardless of the attendee's name.
	@Test
	void rejectsADuplicateEmailEvenWhenTheNamesDiffer() {
		this.attendees.saveAndFlush(new Attendee("Ana", "ana@example.com", null));

		assertThatThrownBy(() -> this.attendees.saveAndFlush(new Attendee("Another Ana", "ana@example.com", null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	// Verifies that Oracle rejects emails that bypass application normalization.
	@ParameterizedTest
	@ValueSource(strings = { "Ana@Example.COM", " ana@example.com " })
	void rejectsEmailsThatWereNotNormalizedBeforeTheWrite(String email) {
		assertThatThrownBy(() -> this.attendees.saveAndFlush(new Attendee("Ana", email, null)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
