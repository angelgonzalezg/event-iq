package com.eventiq.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import com.eventiq.events.EventApiController;
import com.eventiq.events.EventService;
import com.eventiq.payments.TicketTypeApiController;
import com.eventiq.payments.TicketTypeService;

@WebMvcTest({ AuthApiController.class, EventApiController.class, TicketTypeApiController.class })
@Import({ SecurityConfig.class, JwtTokenService.class })
@TestPropertySource(properties = { "ADMIN_PASSWORD=test-password",
	"JWT_SECRET=unit-test-signing-key-with-over-32-characters" })
class ApiSecurityTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JwtDecoder decoder;

	@Autowired
	private JwtEncoder encoder;

	@MockitoBean
	private EventService eventService;

	@MockitoBean
	private TicketTypeService ticketTypeService;

	@Test
	void loginIssuesOnlyAnHs256TokenWithOneHourOfValidity() throws Exception {
		MvcResult result = this.mvc.perform(loginRequest("admin", "test-password"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
			.andReturn();

		Map<String, String> body = JsonPath.read(result.getResponse().getContentAsString(), "$");
		assertThat(body).containsOnlyKeys("token");

		Jwt token = this.decoder.decode(body.get("token"));
		assertThat(token.getSubject()).isEqualTo("admin");
		assertThat(token.getHeaders()).containsEntry("alg", "HS256");
		assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofHours(1));
		assertThat(result.getRequest().getSession(false)).isNull();
	}

	@ParameterizedTest
	@CsvSource({ "admin, wrong-password", "unknown-user, test-password" })
	void rejectsWrongCredentials(String username, String password) throws Exception {
		this.mvc.perform(loginRequest(username, password))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid username or password."))
			.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	void rejectsAnIncompleteLoginRequest() throws Exception {
		this.mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"username":"admin"}
				"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void requiresABearerTokenForBothModules() throws Exception {
		for (String endpoint : List.of("/api/events", "/api/ticket-types", "/api/auth/login")) {
			this.mvc.perform(get(endpoint))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
				.andExpect(header().doesNotExist(HttpHeaders.LOCATION));
		}
	}

	@Test
	void acceptsAnIssuedTokenForBothModules() throws Exception {
		String token = login();

		for (String endpoint : List.of("/api/events", "/api/ticket-types")) {
			MvcResult result = this.mvc.perform(get(endpoint).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();

			assertThat(result.getRequest().getSession(false)).isNull();
		}
	}

	@Test
	void rejectsAnExpiredToken() throws Exception {
		Instant expiredAt = Instant.now().minusSeconds(5);
		JwtClaimsSet claims = JwtClaimsSet.builder().subject("admin").issuedAt(expiredAt.minus(Duration.ofHours(1)))
			.expiresAt(expiredAt).build();
		JwtEncoderParameters parameters = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims);
		String token = this.encoder.encode(parameters).getTokenValue();

		this.mvc.perform(get("/api/events").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsATokenWithAnAlteredSignature() throws Exception {
		String token = login();
		int startOfSignature = token.lastIndexOf('.') + 1;
		char replacement = token.charAt(startOfSignature) == 'A' ? 'B' : 'A';
		String alteredToken = token.substring(0, startOfSignature) + replacement + token.substring(startOfSignature + 1);

		this.mvc.perform(get("/api/events").header(HttpHeaders.AUTHORIZATION, "Bearer " + alteredToken))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void ignoresAnAuthenticatedWebSessionOnTheApi() throws Exception {
		MockHttpSession session = (MockHttpSession) this.mvc
			.perform(post("/login").with(csrf()).param("username", "admin").param("password", "test-password"))
			.andExpect(status().isFound())
			.andReturn().getRequest().getSession(false);
		assertThat(session).isNotNull();

		this.mvc.perform(get("/api/events").session(session))
			.andExpect(status().isUnauthorized());
	}

	/** Gets a token from the actual authentication endpoint for protected requests. */
	private String login() throws Exception {
		MvcResult result = this.mvc.perform(loginRequest("admin", "test-password"))
			.andExpect(status().isOk())
			.andReturn();

		return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}

	/** Builds the login request used by the API clients. */
	private RequestBuilder loginRequest(String username, String password) {
		return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("""
				{"username":"%s","password":"%s"}
				""".formatted(username, password));
	}

}
