package com.eventiq.payments;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(TicketTypeApiController.class)
@WithMockUser
class TicketTypeApiControllerTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private TicketTypeService service;

	@ParameterizedTest
	@MethodSource("validTicketTypes")
	void createsValidTicketTypesIncludingFreeTicketsAndBoundaryValues(String name, String priceValue) throws Exception {
		BigDecimal price = new BigDecimal(priceValue);
		CreateTicketTypeRequest request = new CreateTicketTypeRequest(7L, name, price);
		when(this.service.create(request)).thenReturn(new TicketTypeResponse(21L, 7L, name, price));

		this.mvc.perform(post("/api/ticket-types").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"eventId": 7, "name": "%s", "price": %s}
					""".formatted(name, priceValue)))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.ticketTypeId").value(21))
			.andExpect(jsonPath("$.eventId").value(7))
			.andExpect(jsonPath("$.name").value(name))
			.andExpect(jsonPath("$.price").value(comparesEqualTo(price), BigDecimal.class));
	}

	private static Stream<Arguments> validTicketTypes() {
		return Stream.of(
				Arguments.of("General", "125.75"),
				Arguments.of("Free", "0"),
				Arguments.of("a".repeat(50), "125.75"),
				Arguments.of("Maximum price", "99999999.99"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidTicketTypes")
	void rejectsInvalidFieldsWithoutCallingTheService(String description, String body, String field, String message)
			throws Exception {
		this.mvc.perform(post("/api/ticket-types").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.length()").value(1))
			.andExpect(jsonPath("$.errors[0].field").value(field))
			.andExpect(jsonPath("$.errors[0].message").value(message));
		verifyNoInteractions(this.service);
	}

	private static Stream<Arguments> invalidTicketTypes() {
		return Stream.of(
				Arguments.of("missing name", """
						{"eventId": 7, "price": 125.75}
						""", "name", "Name is required."),
				Arguments.of("null name", """
						{"eventId": 7, "name": null, "price": 125.75}
						""", "name", "Name is required."),
				Arguments.of("empty name", """
						{"eventId": 7, "name": "", "price": 125.75}
						""", "name", "Name is required."),
				Arguments.of("blank name", """
						{"eventId": 7, "name": "   ", "price": 125.75}
						""", "name", "Name is required."),
				Arguments.of("name longer than 50 characters", """
						{"eventId": 7, "name": "%s", "price": 125.75}
						""".formatted("a".repeat(51)), "name", "Name must be at most 50 characters."),
				Arguments.of("missing price", """
						{"eventId": 7, "name": "General"}
						""", "price", "Price is required."),
				Arguments.of("null price", """
						{"eventId": 7, "name": "General", "price": null}
						""", "price", "Price is required."),
				Arguments.of("negative price", """
						{"eventId": 7, "name": "General", "price": -0.01}
						""", "price", "Price must be zero or greater."),
				Arguments.of("more than two decimal places", """
						{"eventId": 7, "name": "General", "price": 10.001}
						""", "price", "Price must have at most 8 integer digits and 2 decimal places."),
				Arguments.of("price above the maximum", """
						{"eventId": 7, "name": "General", "price": 100000000}
						""", "price", "Price must have at most 8 integer digits and 2 decimal places."),
				Arguments.of("missing event ID", """
						{"name": "General", "price": 125.75}
						""", "eventId", "Event is required."));
	}

	@Test
	void listsTicketTypesWithTheirIdsEventsNamesAndPrices() throws Exception {
		when(this.service.findAll()).thenReturn(List.of(
				new TicketTypeResponse(21L, 7L, "General", new BigDecimal("125.75")),
				new TicketTypeResponse(22L, 8L, "Free", BigDecimal.ZERO)));

		this.mvc.perform(get("/api/ticket-types"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					[
					  {"ticketTypeId": 21, "eventId": 7, "name": "General", "price": 125.75},
					  {"ticketTypeId": 22, "eventId": 8, "name": "Free", "price": 0}
					]
					"""));
	}

	@Test
	void returnsAnEmptyJsonArrayWhenNoTicketTypesExist() throws Exception {
		when(this.service.findAll()).thenReturn(List.of());

		this.mvc.perform(get("/api/ticket-types"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("[]"));
	}

	@Test
	void returns404WhenTheRequestedEventDoesNotExist() throws Exception {
		CreateTicketTypeRequest request = new CreateTicketTypeRequest(99L, "General", new BigDecimal("125.75"));
		when(this.service.create(request))
			.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));

		this.mvc.perform(post("/api/ticket-types").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"eventId": 99, "name": "General", "price": 125.75}
					"""))
			.andExpect(status().isNotFound());
	}

}
