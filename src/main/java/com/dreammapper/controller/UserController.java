package com.dreammapper.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dreammapper.dto.ChangePasswordDTO;
import com.dreammapper.dto.UpdateProfileDTO;
import com.dreammapper.dto.UserDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.mapper.UserMapper;
import com.dreammapper.model.User;
import com.dreammapper.repository.UserRepository;
import com.dreammapper.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final UserRepository userRepository;

	@PostMapping
	public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
		User user = UserMapper.toEntity(userDTO);
		User savedUser = userService.saveUser(user);
		return ResponseEntity.ok(UserMapper.toDTO(savedUser));
	}

	@GetMapping
	public ResponseEntity<List<UserDTO>> getAllUsers() {
		List<UserDTO> users = userService.getAllUsers().stream().map(UserMapper::toDTO).collect(Collectors.toList());
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
		return userService.getUserById(id).map(UserMapper::toDTO).map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/profile")
	public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User user = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		User updatedUser = userService.updateProfile(user.getId(), dto);
		return ResponseEntity.ok(UserMapper.toDTO(updatedUser));
	}

	@PutMapping("/password")
	public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordDTO dto,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User user = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		try {
			userService.changePassword(user.getId(), dto);
			return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

}
