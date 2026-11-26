package com.eventiq.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Handles API login and writes the token to the JSON response. */
@RestController
class AuthApiController {

	private final AuthenticationManager authenticationManager;

	private final JwtTokenService jwtTokenService;

	AuthApiController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
		this.authenticationManager = authenticationManager;
		this.jwtTokenService = jwtTokenService;
	}

	/** Exchanges valid credentials for a token without opening a web session. */
	@PostMapping("/api/auth/login")
	ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		Authentication credentials = UsernamePasswordAuthenticationToken.unauthenticated(
			request.username(), request.password());

		Authentication authenticatedUser;
		try {
			authenticatedUser = this.authenticationManager.authenticate(credentials);
		}
		catch (BadCredentialsException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
		}

		String token = this.jwtTokenService.issueToken(authenticatedUser.getName());
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noStore())
			.body(new LoginResponse(token));
	}

	/** Credentials read from the JSON request and validated before login. */
	record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	/** The single field returned in the JSON response. */
	record LoginResponse(String token) {
	}

}
