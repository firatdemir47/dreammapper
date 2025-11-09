package com.dreammapper.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.dreammapper.service.impl.JwtService;

@SpringBootTest
@TestPropertySource(properties = {
		"jwt.secret=test-secret-key-for-jwt-token-generation-minimum-32-bytes",
		"jwt.expiration-ms=3600000",
		"jwt.refresh-expiration-ms=604800000"
})
class JwtServiceTest {

	@Autowired
	private JwtService jwtService;

	@Test
	void testGenerateToken() {
		String token = jwtService.generateToken("test@example.com", Map.of("uid", 1L));
		assertNotNull(token);
		assertTrue(token.length() > 0);
	}

	@Test
	void testExtractSubject() {
		String email = "test@example.com";
		String token = jwtService.generateToken(email, Map.of("uid", 1L));
		String extractedEmail = jwtService.extractSubject(token);
		assertNotNull(extractedEmail);
		assertTrue(extractedEmail.equals(email));
	}

	@Test
	void testExtractUserId() {
		Long userId = 1L;
		String token = jwtService.generateToken("test@example.com", Map.of("uid", userId));
		Long extractedUserId = jwtService.extractUserId(token);
		assertNotNull(extractedUserId);
		assertTrue(extractedUserId.equals(userId));
	}

	@Test
	void testGenerateRefreshToken() {
		String refreshToken = jwtService.generateRefreshToken("test@example.com", Map.of("uid", 1L));
		assertNotNull(refreshToken);
		assertTrue(refreshToken.length() > 0);
		assertTrue(jwtService.isRefreshToken(refreshToken));
	}

	@Test
	void testIsTokenValid() {
		String token = jwtService.generateToken("test@example.com", Map.of("uid", 1L));
		assertTrue(jwtService.isTokenValid(token));
	}
}

