package com.eventiq.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.net.URI;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eventiq.events.AttendeeService;
import com.eventiq.events.EventService;
import com.eventiq.payments.TicketTypeService;

@WebMvcTest
@Import({ SecurityConfig.class, JwtTokenService.class })
@TestPropertySource(properties = { "ADMIN_PASSWORD=test-password",
	"JWT_SECRET=unit-test-signing-key-with-over-32-characters" })
class WebSecurityTests {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EventService eventService;

	@MockitoBean
	private AttendeeService attendeeService;

	@MockitoBean
	private TicketTypeService ticketTypeService;

	@Test
	void redirectsAnAnonymousVisitorToLogin() throws Exception {
		this.mvc.perform(get("/"))
			.andExpect(redirectedUrl("/login"));
	}

	@Test
	void showsAPublicLoginFormWithACsrfToken() throws Exception {
		this.mvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("security/login"))
			.andExpect(content().string(containsString("action=\"/login\"")))
			.andExpect(content().string(containsString("name=\"_csrf\"")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "/css/app.css", "/img/logo.svg" })
	void servesLoginAssetsWithoutASession(String path) throws Exception {
		this.mvc.perform(get(path)).andExpect(status().isOk());
	}

	@Test
	void allowsTheErrorPageWithoutRedirectingToLogin() throws Exception {
		this.mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
			.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
			.andExpect(status().isNotFound())
			.andExpect(view().name("error"));
	}

	@Test
	void signsInAndRendersTheHomePageWithTheSession() throws Exception {
		this.mvc.perform(get("/").session(signIn()))
			.andExpect(status().isOk())
			.andExpect(view().name("home"))
			.andExpect(content().string(containsString("action=\"/logout\"")));
	}

	@Test
	void returnsToTheRequestedPageAfterSignIn() throws Exception {
		MockHttpSession session = (MockHttpSession) this.mvc.perform(get("/events"))
			.andExpect(redirectedUrl("/login"))
			.andReturn().getRequest().getSession(false);

		this.mvc.perform(post("/login").session(session).with(csrf())
			.param("username", "admin").param("password", "test-password"))
			.andExpect(status().isFound())
			.andExpect(result -> assertThat(URI.create(result.getResponse().getRedirectedUrl()).getPath())
				.isEqualTo("/events"));
	}

	@Test
	void showsAnErrorAfterWrongCredentials() throws Exception {
		this.mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "wrong-password"))
			.andExpect(redirectedUrl("/login?error"));

		this.mvc.perform(get("/login?error"))
			.andExpect(content().string(containsString("Invalid username or password.")));
	}

	@Test
	void requiresCsrfOnWebWrites() throws Exception {
		MockHttpSession session = signIn();

		this.mvc.perform(post("/events").session(session))
			.andExpect(status().isForbidden());

		this.mvc.perform(post("/events").session(session).with(csrf())
			.param("name", "Java Day").param("eventDate", "2027-02-14").param("capacity", "50"))
			.andExpect(redirectedUrl("/events"));
	}

	@Test
	void requiresCsrfToSignOutAndInvalidatesTheSession() throws Exception {
		MockHttpSession session = signIn();

		this.mvc.perform(post("/logout").session(session))
			.andExpect(status().isForbidden());
		assertThat(session.isInvalid()).isFalse();

		this.mvc.perform(post("/logout").session(session).with(csrf()))
			.andExpect(redirectedUrl("/login?logout"));
		assertThat(session.isInvalid()).isTrue();

		this.mvc.perform(get("/login?logout"))
			.andExpect(content().string(containsString("You have signed out.")));
	}

	/** Authenticates through the form filter to get a real web session. */
	private MockHttpSession signIn() throws Exception {
		return (MockHttpSession) this.mvc.perform(post("/login").with(csrf())
			.param("username", "admin").param("password", "test-password"))
			.andExpect(redirectedUrl("/"))
			.andReturn().getRequest().getSession(false);
	}

}
