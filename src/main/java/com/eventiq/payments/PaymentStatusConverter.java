package com.eventiq.payments;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

	@Override
	public String convertToDatabaseColumn(PaymentStatus status) {
		return status == null ? null : status.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public PaymentStatus convertToEntityAttribute(String status) {
		if (status == null) {
			return null;
		}
		for (PaymentStatus candidate : PaymentStatus.values()) {
			if (convertToDatabaseColumn(candidate).equals(status)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Unknown payment status: " + status);
	}

}
