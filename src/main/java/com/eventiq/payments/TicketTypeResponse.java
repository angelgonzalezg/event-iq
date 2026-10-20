package com.eventiq.payments;

import java.math.BigDecimal;

public record TicketTypeResponse(Long ticketTypeId, Long eventId, String name, BigDecimal price) {

	static TicketTypeResponse from(TicketType ticketType) {
		return new TicketTypeResponse(ticketType.getTicketTypeId(), ticketType.getEvent().getEventId(),
				ticketType.getName(), ticketType.getPrice());
	}

}
