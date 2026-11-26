package com.eventiq.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Enables Spring Security's web filters and configures the chains used by both modules. */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
class SecurityConfig {

	/** Registers the API filter chain ahead of the general web chain. */
	@Bean
	@Order(1)
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
			.securityMatcher("/api/**")
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// API authentication uses an explicit Bearer header, independent of browser cookies.
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			.build();
	}

	/** Protects views with session-based login and retains Spring Security's CSRF protection. */
	@Bean
	@Order(2)
	SecurityFilterChain webSecurityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager)
			throws Exception {
		return http
			.authenticationManager(authenticationManager)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/css/**", "/img/**", "/error").permitAll()
				.anyRequest().authenticated())
			.formLogin(form -> form
				.loginPage("/login")
				.defaultSuccessUrl("/")
				.permitAll())
			.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
			.build();
	}

	/** Creates the encoder used to hash and check the administrator's password. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}

	/** Creates the administrator, using an empty default to report missing configuration clearly. */
	@Bean
	InMemoryUserDetailsManager adminUser(@Value("${ADMIN_PASSWORD:}") String password, PasswordEncoder passwordEncoder) {
		// Reject the placeholder from .env.example as well as a blank password.
		if (password.isBlank() || password.equals("<app_login_password>")) {
			throw new IllegalStateException("Set ADMIN_PASSWORD to the administrator's password.");
		}

		String passwordHash = passwordEncoder.encode(password);
		UserDetails admin = User.withUsername("admin")
			.password(passwordHash)
			.authorities("ROLE_ADMIN")
			.build();

		return new InMemoryUserDetailsManager(admin);
	}

	/** Uses Spring's username/password provider with the in-memory account and password encoder. */
	@Bean
	AuthenticationManager authenticationManager(InMemoryUserDetailsManager users, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
		provider.setPasswordEncoder(passwordEncoder);

		return new ProviderManager(provider);
	}

	/** Fails at startup if the signing secret is absent, too short or still the example value. */
	@Bean
	SecretKey jwtSigningKey(@Value("${JWT_SECRET:}") String secret) {
		if (secret.isBlank() || secret.codePointCount(0, secret.length()) < 32
				|| secret.equals("<at_least_32_random_characters>")) {
			throw new IllegalStateException("Set JWT_SECRET to at least 32 random characters.");
		}

		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	/** Uses Spring Security's Nimbus-backed encoder to sign API tokens with HS256. */
	@Bean
	JwtEncoder jwtEncoder(SecretKey key) {
		return NimbusJwtEncoder.withSecretKey(key)
			.algorithm(MacAlgorithm.HS256)
			.build();
	}

	/** Checks the HS256 signature and requires a current expiry on every API token. */
	@Bean
	JwtDecoder jwtDecoder(SecretKey key) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();

		JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ZERO);
		timestamp.setAllowEmptyExpiryClaim(false);
		decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(List.of(timestamp)));

		return decoder;
	}

}
