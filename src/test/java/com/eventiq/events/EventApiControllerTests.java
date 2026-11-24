package com.eventiq.events;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

/**
 * Covers the acceptance criteria of the event CRUD at the HTTP level. The error responses come
 * from {@code ApiExceptionHandler}, which {@code @WebMvcTest} loads together with the controller.
 */
@WebMvcTest(EventApiController.class)
@WithMockUser
class EventApiControllerTests {

	private static final String VALID_BODY = """
			{"name": "Java Day", "description": "Talks", "eventDate": "2026-11-10", "location": "Online", "capacity": 50}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EventService service;

	@Test
	void createsAnEventAndAnswers201WithItsLocationAndData() throws Exception {
		when(this.service.create(any(EventRequest.class))).thenReturn(response(7L));

		this.mvc.perform(post("/api/events").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", endsWith("/api/events/7")))
			.andExpect(jsonPath("$.eventId").value(7))
			.andExpect(jsonPath("$.name").value("Java Day"))
			.andExpect(jsonPath("$.eventDate").value("2026-11-10"))
			.andExpect(jsonPath("$.capacity").value(50));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidEvents")
	void rejectsInvalidDataWith400AndTheFieldNameWithoutCallingTheService(String description, String body,
			String field) throws Exception {
		this.mvc.perform(post("/api/events").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors[*].field", hasItem(field)));
		verifyNoInteractions(this.service);
	}

	private static Stream<Arguments> invalidEvents() {
		return Stream.of(
				Arguments.of("missing name", """
						{"eventDate": "2026-11-10", "capacity": 50}
						""", "name"),
				Arguments.of("blank name", """
						{"name": "   ", "eventDate": "2026-11-10", "capacity": 50}
						""", "name"),
				Arguments.of("name longer than 150 characters", """
						{"name": "%s", "eventDate": "2026-11-10", "capacity": 50}
						""".formatted("a".repeat(151)), "name"),
				Arguments.of("description longer than 2000 characters", """
						{"name": "Java Day", "description": "%s", "eventDate": "2026-11-10", "capacity": 50}
						""".formatted("a".repeat(2001)), "description"),
				Arguments.of("missing date", """
						{"name": "Java Day", "capacity": 50}
						""", "eventDate"),
				Arguments.of("capacity of zero", """
						{"name": "Java Day", "eventDate": "2026-11-10", "capacity": 0}
						""", "capacity"),
				Arguments.of("negative capacity", """
						{"name": "Java Day", "eventDate": "2026-11-10", "capacity": -5}
						""", "capacity"),
				Arguments.of("missing capacity", """
						{"name": "Java Day", "eventDate": "2026-11-10"}
						""", "capacity"));
	}

	@Test
	void reportsEveryInvalidFieldWithItsMessage() throws Exception {
		this.mvc.perform(post("/api/events").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "", "eventDate": "2026-11-10", "capacity": 0}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.length()").value(2))
			.andExpect(jsonPath("$.errors[*].message",
					hasItems("Name is required.", "Capacity must be greater than zero.")));
	}

	@Test
	void answers400WhenTheBodyIsNotValidJson() throws Exception {
		this.mvc.perform(post("/api/events").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{not json"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		verifyNoInteractions(this.service);
	}

	@Test
	void answers400WhenTheDateIsNotARealDate() throws Exception {
		this.mvc.perform(post("/api/events").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Java Day", "eventDate": "2026-13-45", "capacity": 50}
					"""))
			.andExpect(status().isBadRequest());
		verifyNoInteractions(this.service);
	}

	@Test
	void listsEvents() throws Exception {
		when(this.service.findAll()).thenReturn(List.of(response(7L)));

		this.mvc.perform(get("/api/events"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					[{"eventId": 7, "name": "Java Day", "description": "Talks", "eventDate": "2026-11-10",
					  "location": "Online", "capacity": 50}]
					"""));
	}

	@Test
	void returnsAnEmptyJsonArrayWhenNoEventsExist() throws Exception {
		when(this.service.findAll()).thenReturn(List.of());

		this.mvc.perform(get("/api/events")).andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void returnsAnExistingEventById() throws Exception {
		when(this.service.findById(7L)).thenReturn(response(7L));

		this.mvc.perform(get("/api/events/7"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.eventId").value(7))
			.andExpect(jsonPath("$.name").value("Java Day"));
	}

	@Test
	void answers404WhenTheRequestedEventDoesNotExist() throws Exception {
		when(this.service.findById(99L)).thenThrow(notFound());

		this.mvc.perform(get("/api/events/99"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("Event not found."));
	}

	@Test
	void updatesAnEvent() throws Exception {
		when(this.service.update(any(Long.class), any(EventRequest.class))).thenReturn(response(7L));

		this.mvc.perform(put("/api/events/7").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.eventId").value(7));
	}

	@Test
	void rejectsAnInvalidUpdateWith400() throws Exception {
		this.mvc.perform(put("/api/events/7").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Java Day", "eventDate": "2026-11-10", "capacity": 0}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field", hasItem("capacity")));
		verifyNoInteractions(this.service);
	}

	@Test
	void answers404WhenUpdatingAnEventThatDoesNotExist() throws Exception {
		when(this.service.update(any(Long.class), any(EventRequest.class))).thenThrow(notFound());

		this.mvc.perform(put("/api/events/99").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isNotFound());
	}

	@Test
	void deletesAnEventAndAnswers204() throws Exception {
		this.mvc.perform(delete("/api/events/7").with(csrf())).andExpect(status().isNoContent());

		verify(this.service).delete(7L);
	}

	@Test
	void answers404WhenDeletingAnEventThatDoesNotExist() throws Exception {
		doThrow(notFound()).when(this.service).delete(99L);

		this.mvc.perform(delete("/api/events/99").with(csrf())).andExpect(status().isNotFound());
	}

	@Test
	void answers409WithAClearMessageWhenTheEventHasRegistrations() throws Exception {
		doThrow(new EventHasRegistrationsException()).when(this.service).delete(7L);

		this.mvc.perform(delete("/api/events/7").with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("The event has registrations and cannot be deleted."));
	}

	@Test
	void answers409InsteadOfA500WhenTheDatabaseRefusesTheDelete() throws Exception {
		doThrow(new DataIntegrityViolationException("FK_TICKET_TYPES_EVENT")).when(this.service).delete(8L);

		this.mvc.perform(delete("/api/events/8").with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private static EventResponse response(Long id) {
		return new EventResponse(id, "Java Day", "Talks", LocalDate.of(2026, 11, 10), "Online", 50);
	}

	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.");
	}
}
