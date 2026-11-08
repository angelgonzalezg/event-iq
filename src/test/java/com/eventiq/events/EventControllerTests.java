package com.eventiq.events;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(EventController.class)
@WithMockUser
class EventControllerTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EventService service;

	@Test
	void listShowsTheEventsInATable() throws Exception {
		when(this.service.findAll()).thenReturn(List.of(response(7L)));

		this.mvc.perform(get("/events"))
			.andExpect(status().isOk())
			.andExpect(view().name("events/list"))
			.andExpect(model().attribute("events", hasSize(1)))
			.andExpect(content().string(containsString("Java Day")))
			.andExpect(content().string(containsString("2026-11-10")))
			.andExpect(content().string(containsString("href=\"/events/7/edit\"")))
			.andExpect(content().string(containsString("action=\"/events/7/delete\"")));
	}

	@Test
	void listShowsAnEmptyStateWhenThereAreNoEvents() throws Exception {
		when(this.service.findAll()).thenReturn(List.of());

		this.mvc.perform(get("/events"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("No events yet")));
	}

	@Test
	void newFormPostsToTheCollection() throws Exception {
		this.mvc.perform(get("/events/new"))
			.andExpect(status().isOk())
			.andExpect(view().name("events/form"))
			.andExpect(content().string(containsString("action=\"/events\"")));
	}

	@Test
	void editFormIsFilledWithTheEventAndPostsToItsId() throws Exception {
		when(this.service.findById(7L)).thenReturn(response(7L));

		this.mvc.perform(get("/events/7/edit"))
			.andExpect(status().isOk())
			.andExpect(view().name("events/form"))
			.andExpect(content().string(containsString("action=\"/events/7\"")))
			.andExpect(content().string(containsString("value=\"Java Day\"")));
	}

	@Test
	void editFormAnswers404WhenTheEventDoesNotExist() throws Exception {
		when(this.service.findById(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found."));

		this.mvc.perform(get("/events/99/edit")).andExpect(status().isNotFound());
	}

	@Test
	void createRedirectsToTheListWithASuccessMessage() throws Exception {
		this.mvc.perform(post("/events").with(csrf())
			.param("name", "Java Day")
			.param("eventDate", "2026-11-10")
			.param("capacity", "50"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/events"))
			.andExpect(flash().attribute("successMessage", "events.created"));

		verify(this.service).create(any(EventRequest.class));
	}

	@Test
	void createShowsFieldErrorsAndDoesNotSaveWhenTheDataIsInvalid() throws Exception {
		this.mvc.perform(post("/events").with(csrf())
			.param("name", "")
			.param("eventDate", "2026-11-10")
			.param("capacity", "0"))
			.andExpect(status().isOk())
			.andExpect(view().name("events/form"))
			.andExpect(model().attributeHasFieldErrors("event", "name", "capacity"))
			.andExpect(content().string(containsString("Name is required.")))
			.andExpect(content().string(containsString("Capacity must be greater than zero.")));

		verify(this.service, never()).create(any(EventRequest.class));
	}

	@Test
	void updateRedirectsToTheListWithASuccessMessage() throws Exception {
		this.mvc.perform(post("/events/7").with(csrf())
			.param("name", "Java Day 2")
			.param("eventDate", "2026-12-01")
			.param("capacity", "80"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/events"))
			.andExpect(flash().attribute("successMessage", "events.updated"));

		verify(this.service).update(any(Long.class), any(EventRequest.class));
	}

	@Test
	void updateShowsFieldErrorsAndKeepsPostingToTheSameEventWhenTheDataIsInvalid() throws Exception {
		this.mvc.perform(post("/events/7").with(csrf())
			.param("name", "")
			.param("eventDate", "2026-12-01")
			.param("capacity", "80"))
			.andExpect(status().isOk())
			.andExpect(view().name("events/form"))
			.andExpect(model().attributeHasFieldErrors("event", "name"))
			.andExpect(content().string(containsString("action=\"/events/7\"")));

		verify(this.service, never()).update(any(Long.class), any(EventRequest.class));
	}

	@Test
	void deleteRedirectsToTheListWithASuccessMessage() throws Exception {
		this.mvc.perform(post("/events/7/delete").with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/events"))
			.andExpect(flash().attribute("successMessage", "events.deleted"));

		verify(this.service).delete(7L);
	}

	@Test
	void deleteShowsAClearErrorWhenTheEventHasRegistrations() throws Exception {
		doThrow(new EventHasRegistrationsException()).when(this.service).delete(7L);

		this.mvc.perform(post("/events/7/delete").with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/events"))
			.andExpect(flash().attribute("errorMessage", "events.delete.has-registrations"));
	}

	@Test
	void deleteShowsAClearErrorWhenTheDatabaseRefusesTheDelete() throws Exception {
		doThrow(new DataIntegrityViolationException("FK_TICKET_TYPES_EVENT")).when(this.service).delete(8L);

		this.mvc.perform(post("/events/8/delete").with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/events"))
			.andExpect(flash().attribute("errorMessage", "events.delete.in-use"));
	}

	private static EventResponse response(Long id) {
		return new EventResponse(id, "Java Day", "Talks", LocalDate.of(2026, 11, 10), "Online", 50);
	}
}