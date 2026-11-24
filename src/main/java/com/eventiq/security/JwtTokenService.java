package com.eventiq.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
class JwtTokenService {

	private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);

	private final JwtEncoder jwtEncoder;

	JwtTokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	/** Issues a one-hour bearer token for the authenticated administrator. */
	String issueToken(String username) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(TOKEN_LIFETIME);

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(username)
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		JwtEncoderParameters parameters = JwtEncoderParameters.from(header, claims);
		return this.jwtEncoder.encode(parameters).getTokenValue();
	}

}
