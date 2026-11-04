package com.eventiq.payments;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketTypeRequest(
		@NotNull(message = "Event is required.") Long eventId,
		@NotBlank(message = "Name is required.")
		@Size(max = 50, message = "Name must be at most 50 characters.") String name,
		@NotNull(message = "Price is required.")
		@DecimalMin(value = "0.00", message = "Price must be zero or greater.")
		@Digits(integer = 8, fraction = 2,
				message = "Price must have at most 8 integer digits and 2 decimal places.") BigDecimal price) {

}
