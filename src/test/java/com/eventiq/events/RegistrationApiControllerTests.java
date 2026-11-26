package com.eventiq.events;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@WebMvcTest(RegistrationApiController.class)
@WithMockUser
class RegistrationApiControllerTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistrationService service;

	private static final String BODY = """
			{"eventId": 1, "attendeeId": 2}
			""";

	@Test
	void createsAPendingRegistrationAndAnswers201() throws Exception {
		when(this.service.register(new CreateRegistrationRequest(1L, 2L)))
			.thenReturn(new RegistrationResponse(9L, 1L, 2L, "pending", LocalDateTime.of(2026, 11, 1, 10, 0)));

		this.mvc.perform(post("/api/registrations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.registrationId").value(9))
			.andExpect(jsonPath("$.status").value("pending"));
	}

	@Test
	void answers409WhenTheEventIsFull() throws Exception {
		when(this.service.register(any())).thenThrow(new EventFullException());

		this.mvc.perform(post("/api/registrations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("The event is full."));
	}

	@Test
	void answers409WhenTheAttendeeIsAlreadyRegistered() throws Exception {
		when(this.service.register(any())).thenThrow(new AlreadyRegisteredException());

		this.mvc.perform(post("/api/registrations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("The attendee is already registered for this event."));
	}

	@Test
	void answers400WhenAnIdIsMissing() throws Exception {
		this.mvc.perform(post("/api/registrations").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"eventId\": 1}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Attendee id is required.")));
		verifyNoInteractions(this.service);
	}

	@Test
	void cancelsARegistrationAndAnswers204() throws Exception {
		this.mvc.perform(delete("/api/registrations/9").with(csrf())).andExpect(status().isNoContent());

		verify(this.service).cancel(9L);
	}

	@Test
	void answers404WhenTheRegistrationDoesNotExist() throws Exception {
		when(this.service.cancel(9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found."));

		this.mvc.perform(delete("/api/registrations/9").with(csrf())).andExpect(status().isNotFound());
	}

}
