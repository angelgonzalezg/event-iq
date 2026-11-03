package com.eventiq.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentStatusConverterTests {

	private final PaymentStatusConverter converter = new PaymentStatusConverter();

	@Test
	void storesEachStatusInLowercase() {
		assertThat(this.converter.convertToDatabaseColumn(PaymentStatus.PENDING)).isEqualTo("pending");
		assertThat(this.converter.convertToDatabaseColumn(PaymentStatus.PAID)).isEqualTo("paid");
		assertThat(this.converter.convertToDatabaseColumn(PaymentStatus.REFUNDED)).isEqualTo("refunded");
	}

	@Test
	void readsEveryStatusItStores() {
		for (PaymentStatus status : PaymentStatus.values()) {
			String column = this.converter.convertToDatabaseColumn(status);

			assertThat(this.converter.convertToEntityAttribute(column)).isEqualTo(status);
		}
	}

	@Test
	void keepsNullAsNull() {
		assertThat(this.converter.convertToDatabaseColumn(null)).isNull();
		assertThat(this.converter.convertToEntityAttribute(null)).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "unknown", "PENDING", "", " ", " pending " })
	void rejectsInvalidStatusesWithAClearMessage(String status) {
		assertThatIllegalArgumentException().isThrownBy(() -> this.converter.convertToEntityAttribute(status))
			.withMessage("The payment status is invalid. Allowed values: pending, paid, refunded.");
	}

}
