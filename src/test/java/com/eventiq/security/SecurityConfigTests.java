package com.eventiq.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class SecurityConfigTests {

	private final WebApplicationContextRunner context = new WebApplicationContextRunner()
		.withUserConfiguration(SecurityConfig.class);

	@ParameterizedTest
	@ValueSource(strings = { "", "short", "<at_least_32_random_characters>" })
	void failsToStartWithAnInvalidSigningSecret(String secret) {
		this.context.withPropertyValues("ADMIN_PASSWORD=test-password", "JWT_SECRET=" + secret)
			.run(app -> assertThat(app.getStartupFailure()).hasRootCauseMessage(
				"Set JWT_SECRET to at least 32 random characters."));
	}

	@Test
	void failsToStartWithoutAnAdminPassword() {
		this.context.withPropertyValues("ADMIN_PASSWORD=", "JWT_SECRET=unit-test-signing-key-with-over-32-characters")
			.run(app -> assertThat(app.getStartupFailure()).hasRootCauseMessage(
				"Set ADMIN_PASSWORD to the administrator's password."));
	}

}
