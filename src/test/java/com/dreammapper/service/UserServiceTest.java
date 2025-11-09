package com.dreammapper.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.dreammapper.dto.UpdateProfileDTO;
import com.dreammapper.model.User;
import com.dreammapper.service.UserService;

@SpringBootTest
@TestPropertySource(properties = {
		"jwt.secret=test-secret-key-for-jwt-token-generation-minimum-32-bytes"
})
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Test
	void testSaveUser() {
		User user = User.builder()
				.name("Test User")
				.email("testuser@example.com")
				.password("password123")
				.build();

		User savedUser = userService.saveUser(user);
		assertNotNull(savedUser);
		assertNotNull(savedUser.getId());
	}

	@Test
	void testGetUserByEmail() {
		User user = User.builder()
				.name("Test User 2")
				.email("testuser2@example.com")
				.password("password123")
				.build();

		userService.saveUser(user);
		Optional<User> foundUser = userService.getUserByEmail("testuser2@example.com");
		assertTrue(foundUser.isPresent());
		assertTrue(foundUser.get().getEmail().equals("testuser2@example.com"));
	}

	@Test
	void testUpdateProfile() {
		User user = User.builder()
				.name("Test User 3")
				.email("testuser3@example.com")
				.password("password123")
				.build();

		User savedUser = userService.saveUser(user);

		UpdateProfileDTO updateDTO = UpdateProfileDTO.builder()
				.name("Updated Name")
				.email("updated@example.com")
				.build();

		User updatedUser = userService.updateProfile(savedUser.getId(), updateDTO);
		assertNotNull(updatedUser);
		assertTrue(updatedUser.getName().equals("Updated Name"));
	}
}

