package com.eventiq.events;

import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RegistrationStatusConverter implements AttributeConverter<RegistrationStatus, String> {

	@Override
	public String convertToDatabaseColumn(RegistrationStatus status) {
		return status == null ? null : status.name().toLowerCase(Locale.ROOT);
	}

	@Override
	public RegistrationStatus convertToEntityAttribute(String status) {
		if (status == null) {
			return null;
		}
		for (RegistrationStatus candidate : RegistrationStatus.values()) {
			if (convertToDatabaseColumn(candidate).equals(status)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Unknown registration status: " + status);
	}

}
