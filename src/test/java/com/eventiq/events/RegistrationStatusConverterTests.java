package com.eventiq.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * The statuses stored in {@code registrations.status} must match the values allowed by the
 * {@code ck_registrations_status} constraint in {@code db/schema.sql}.
 */
class RegistrationStatusConverterTests {

	private final RegistrationStatusConverter converter = new RegistrationStatusConverter();

	@Test
	void storesEachStatusInLowercase() {
		assertThat(this.converter.convertToDatabaseColumn(RegistrationStatus.PENDING)).isEqualTo("pending");
		assertThat(this.converter.convertToDatabaseColumn(RegistrationStatus.CONFIRMED)).isEqualTo("confirmed");
		assertThat(this.converter.convertToDatabaseColumn(RegistrationStatus.CANCELLED)).isEqualTo("cancelled");
	}

	@Test
	void readsEveryStatusItStores() {
		for (RegistrationStatus status : RegistrationStatus.values()) {
			String column = this.converter.convertToDatabaseColumn(status);

			assertThat(this.converter.convertToEntityAttribute(column)).isEqualTo(status);
		}
	}

	@Test
	void keepsNullAsNull() {
		assertThat(this.converter.convertToDatabaseColumn(null)).isNull();
		assertThat(this.converter.convertToEntityAttribute(null)).isNull();
	}

	@Test
	void rejectsAnUnknownStatus() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.converter.convertToEntityAttribute("PENDING"))
			.withMessageContaining("PENDING");
	}

}
