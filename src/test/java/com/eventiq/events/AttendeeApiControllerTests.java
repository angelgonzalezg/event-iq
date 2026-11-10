package com.eventiq.events;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.stream.Stream;

import com.eventiq.payments.TicketTypeApiController;
import com.eventiq.payments.TicketTypeService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest({ AttendeeApiController.class, TicketTypeApiController.class })
@WithMockUser
class AttendeeApiControllerTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AttendeeService service;

	@MockitoBean
	private TicketTypeService ticketTypes;

	@Test
	void createsAnAttendeeAndItsLocationCanBeUsedToLookItUp() throws Exception {
		CreateAttendeeRequest request = new CreateAttendeeRequest("Ana O'León", "ana.lopez+news@example.com", "+52 81 1234 5678");
		AttendeeResponse response = new AttendeeResponse(7L, request.name(), request.email(), request.phone());
		when(this.service.create(request)).thenReturn(response);
		when(this.service.findByEmail(request.email())).thenReturn(response);

		String location = this.mvc.perform(post("/api/attendees").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Ana O'León", "email": "  ANA.LOPEZ+news@Example.COM  ", "phone": "+52 81 1234 5678"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "http://localhost/api/attendees/ana.lopez%2Bnews%40example.com"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.attendeeId").value(7))
			.andExpect(jsonPath("$.name").value(request.name()))
			.andExpect(jsonPath("$.email").value(request.email()))
			.andExpect(jsonPath("$.phone").value(request.phone()))
			.andReturn().getResponse().getHeader("Location");

		this.mvc.perform(get(URI.create(location)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.attendeeId").value(7))
			.andExpect(jsonPath("$.email").value(request.email()));
	}

	@Test
	void acceptsAnOmittedPhone() throws Exception {
		when(this.service.create(new CreateAttendeeRequest("Ana", "ana@example.com", null)))
			.thenReturn(new AttendeeResponse(7L, "Ana", "ana@example.com", null));

		this.mvc.perform(post("/api/attendees").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Ana", "email": "ana@example.com"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.attendeeId").value(7))
			.andExpect(jsonPath("$.phone").isEmpty());
	}

	@Test
	void acceptsTheSchemaLengthLimits() throws Exception {
		String name = "a".repeat(100);
		String email = "a".repeat(64) + "@" + "b".repeat(31) + ".com";
		String phone = "1".repeat(20);
		when(this.service.create(new CreateAttendeeRequest(name, email, phone)))
			.thenReturn(new AttendeeResponse(7L, name, email, phone));

		this.mvc.perform(post("/api/attendees").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "%s", "email": "%s", "phone": "%s"}
					""".formatted(name, email, phone)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value(email));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidAttendees")
	void rejectsInvalidFieldsWith400WithoutCallingTheService(String description, String body, String field) throws Exception {
		this.mvc.perform(post("/api/attendees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors[*].field", hasItem(field)));
		verifyNoInteractions(this.service);
	}

	private static Stream<Arguments> invalidAttendees() {
		return Stream.of(
				Arguments.of("missing name", """
						{"email": "ana@example.com"}
						""", "name"),
				Arguments.of("null name", """
						{"name": null, "email": "ana@example.com"}
						""", "name"),
				Arguments.of("blank name", """
						{"name": "   ", "email": "ana@example.com"}
						""", "name"),
				Arguments.of("name longer than 100", """
						{"name": "%s", "email": "ana@example.com"}
						""".formatted("a".repeat(101)), "name"),
				Arguments.of("missing email", """
						{"name": "Ana"}
						""", "email"),
				Arguments.of("null email", """
						{"name": "Ana", "email": null}
						""", "email"),
				Arguments.of("empty email", """
						{"name": "Ana", "email": ""}
						""", "email"),
				Arguments.of("blank email", """
						{"name": "Ana", "email": "   "}
						""", "email"),
				Arguments.of("malformed email", """
						{"name": "Ana", "email": "not-an-email"}
						""", "email"),
				Arguments.of("internal email whitespace", """
						{"name": "Ana", "email": "ana lopez@example.com"}
						""", "email"),
				Arguments.of("email longer than 100", """
						{"name": "Ana", "email": "%s"}
						""".formatted("a".repeat(64) + "@" + "b".repeat(32) + ".com"), "email"),
				Arguments.of("phone longer than 20", """
						{"name": "Ana", "email": "ana@example.com", "phone": "%s"}
						""".formatted("1".repeat(21)), "phone"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "{not json", "null", "[]" })
	void rejectsMissingOrMalformedBodies(String body) throws Exception {
		this.mvc.perform(post("/api/attendees").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("The request body is missing or malformed."));
		verifyNoInteractions(this.service);
	}

	@Test
	void answers409WithAClearMessageForADuplicateEmail() throws Exception {
		when(this.service.create(any(CreateAttendeeRequest.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "An attendee with this email already exists."));

		this.mvc.perform(post("/api/attendees").with(csrf()).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Ana", "email": "ana@example.com"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("An attendee with this email already exists."));
	}

	@Test
	void answers404WithAClearMessageWhenTheEmailIsUnknown() throws Exception {
		when(this.service.findByEmail("missing@example.com"))
			.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found."));

		this.mvc.perform(get("/api/attendees/{email}", "missing@example.com"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("Attendee not found."));
	}

	@Test
	void doesNotApplyTheAttendeeErrorHandlerToOtherControllers() throws Exception {
		when(this.ticketTypes.findAll()).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));

		this.mvc.perform(get("/api/ticket-types"))
			.andExpect(status().isNotFound())
			.andExpect(content().string(""));
	}

}
