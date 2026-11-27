package com.eventiq.events;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(RegistrationController.class)
@WithMockUser
class RegistrationControllerTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrationService registrations;

	@MockitoBean
	private EventService events;

	@MockitoBean
	private AttendeeService attendees;

	private void eventsExist() {
		when(this.events.findAll())
			.thenReturn(List.of(new EventResponse(1L, "Java Day", null, LocalDate.of(2026, 11, 10), "Online", 2)));
	}

	@Test
	void showsTheFormWithTheEventsAndPreselectsTheRequestedOne() throws Exception {
		eventsExist();

		this.mvc.perform(get("/registrations/new").param("eventId", "1"))
			.andExpect(status().isOk())
			.andExpect(view().name("registrations/form"))
			.andExpect(content().string(containsString("action=\"/registrations\"")))
			.andExpect(content().string(containsString("Java Day")));
	}

	@Test
	void registersTheAttendeeAndGoesToTheEventDetail() throws Exception {
		when(this.attendees.findByEmail("ana@example.com")).thenReturn(new AttendeeResponse(2L, "Ana", "ana@example.com", null));
		when(this.registrations.register(new CreateRegistrationRequest(1L, 2L)))
			.thenReturn(new RegistrationResponse(9L, 1L, 2L, "pending", LocalDateTime.now()));

		this.mvc.perform(post("/registrations").with(csrf()).param("eventId", "1").param("email", "ana@example.com"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/events/1"))
			.andExpect(flash().attribute("successMessage", "registrations.created"));
	}

	@Test
	void tellsTheUserWhenTheEventIsFull() throws Exception {
		when(this.attendees.findByEmail("ana@example.com")).thenReturn(new AttendeeResponse(2L, "Ana", "ana@example.com", null));
		when(this.registrations.register(any())).thenThrow(new EventFullException());

		this.mvc.perform(post("/registrations").with(csrf()).param("eventId", "1").param("email", "ana@example.com"))
			.andExpect(redirectedUrl("/registrations/new?eventId=1"))
			.andExpect(flash().attribute("errorMessage", "registrations.event-full"));
	}

	@Test
	void tellsTheUserWhenTheAttendeeIsAlreadyRegistered() throws Exception {
		when(this.attendees.findByEmail("ana@example.com")).thenReturn(new AttendeeResponse(2L, "Ana", "ana@example.com", null));
		when(this.registrations.register(any())).thenThrow(new AlreadyRegisteredException());

		this.mvc.perform(post("/registrations").with(csrf()).param("eventId", "1").param("email", "ana@example.com"))
			.andExpect(redirectedUrl("/registrations/new?eventId=1"))
			.andExpect(flash().attribute("errorMessage", "registrations.already-registered"));
	}

	@Test
	void showsAFieldErrorWhenNoAttendeeUsesTheEmail() throws Exception {
		eventsExist();
		when(this.attendees.findByEmail("nobody@example.com"))
			.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendee not found."));

		this.mvc.perform(post("/registrations").with(csrf()).param("eventId", "1").param("email", "nobody@example.com"))
			.andExpect(status().isOk())
			.andExpect(view().name("registrations/form"))
			.andExpect(content().string(containsString("No attendee uses this email.")));
		verifyNoInteractions(this.registrations);
	}

	@Test
	void redisplaysTheFormWhenTheFieldsAreMissing() throws Exception {
		eventsExist();

		this.mvc.perform(post("/registrations").with(csrf()))
			.andExpect(status().isOk())
			.andExpect(view().name("registrations/form"))
			.andExpect(content().string(containsString("Event is required.")))
			.andExpect(content().string(containsString("Email is required.")));
		verifyNoInteractions(this.registrations);
	}

	@Test
	void cancelsAndReturnsToTheEventDetail() throws Exception {
		when(this.registrations.cancel(9L)).thenReturn(new RegistrationResponse(9L, 1L, 2L, "cancelled", LocalDateTime.now()));

		this.mvc.perform(post("/registrations/9/cancel").with(csrf()))
			.andExpect(redirectedUrl("/events/1"))
			.andExpect(flash().attribute("successMessage", "registrations.cancelled"));
		verify(this.registrations).cancel(9L);
	}

}
