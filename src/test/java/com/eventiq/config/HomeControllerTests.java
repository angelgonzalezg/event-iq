package com.eventiq.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Renders the home page through the shared layout. {@code @WithMockUser} keeps these tests
 * independent of the security configuration that EIQ-29 introduces.
 */
@WebMvcTest(HomeController.class)
@WithMockUser
class HomeControllerTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void homeUsesTheLayoutAndLinksEveryModuleView() throws Exception {
		this.mvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("home"))
			.andExpect(content().string(containsString("<title>Home · EventIQ</title>")))
			.andExpect(content().string(containsString("href=\"/events\"")))
			.andExpect(content().string(containsString("href=\"/registrations/new\"")))
			.andExpect(content().string(containsString("href=\"/assistant\"")))
			.andExpect(content().string(containsString("href=\"/payments\"")))
			.andExpect(content().string(containsString("href=\"/payments/new\"")))
			.andExpect(content().string(containsString("href=\"/analytics\"")));
	}

	@Test
	void layoutResolvesAlertMessageKeys() throws Exception {
		this.mvc.perform(get("/").flashAttr("successMessage", "home.heading"))
			.andExpect(content().string(containsString("alert-success")))
			.andExpect(content().string(not(containsString("home.heading"))));
	}

	@Test
	void layoutShowsAlertTextAsIs() throws Exception {
		this.mvc.perform(get("/").flashAttr("serviceWarning", "The analytics service is not responding."))
			.andExpect(content().string(containsString("alert-warning")))
			.andExpect(content().string(containsString("The analytics service is not responding.")));
	}

	@Test
	void signedInUsersGetACsrfProtectedSignOutForm() throws Exception {
		this.mvc.perform(get("/"))
			.andExpect(content().string(containsString("action=\"/logout\"")))
			.andExpect(content().string(containsString("name=\"_csrf\"")));
	}

}
