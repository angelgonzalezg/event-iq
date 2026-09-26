package com.eventiq.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary development security: permits every request and disables CSRF protection on
 * {@code /api/**}, so the CRUD endpoints can be tested from Postman. CSRF stays
 * enabled for the Thymeleaf views.
 *
 * <p>Do not add rules here. The security story (EIQ-29) deletes this class and replaces it
 * with form login for the views and JWT for {@code /api/**}.
 */
// TODO(EIQ-29): delete this class once form login and JWT are in place.
@Configuration
public class DevSecurityConfig {

	@Bean
	SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
				.build();
	}

}
