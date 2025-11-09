package com.dreammapper.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"jwt.secret=test-secret-key-for-jwt-token-generation-minimum-32-bytes"
})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void testRegisterEndpoint() throws Exception {
		String requestBody = """
				{
					"email": "test@example.com",
					"password": "password123"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isOk());
	}

	@Test
	void testRegisterWithInvalidEmail() throws Exception {
		String requestBody = """
				{
					"email": "invalid-email",
					"password": "password123"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isBadRequest());
	}
}

