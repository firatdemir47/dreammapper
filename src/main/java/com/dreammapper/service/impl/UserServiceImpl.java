package com.dreammapper.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dreammapper.dto.ChangePasswordDTO;
import com.dreammapper.dto.UpdateProfileDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.model.User;
import com.dreammapper.repository.UserRepository;
import com.dreammapper.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

	@Override
	public User saveUser(User user) {
		if (user.getPassword() != null) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}
		return userRepository.save(user);
	}

	@Override
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@Override
	public Optional<User> getUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	public void deleteUser(Long id) {
		userRepository.deleteById(id);

	}

	@Override
	public User updateProfile(Long userId, UpdateProfileDTO dto) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		// Check if email is already taken by another user
		if (!user.getEmail().equals(dto.getEmail())) {
			Optional<User> existingUser = userRepository.findByEmail(dto.getEmail());
			if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
				throw new IllegalArgumentException("Email already in use");
			}
		}

		user.setName(dto.getName());
		user.setEmail(dto.getEmail());
		return userRepository.save(user);
	}

	@Override
	public void changePassword(Long userId, ChangePasswordDTO dto) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		// Verify current password
		if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Current password is incorrect");
		}

		// Check if new password is different from current password
		if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
			throw new IllegalArgumentException("New password must be different from current password");
		}

		// Update password
		user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
		userRepository.save(user);
	}

	@Override
	public Optional<User> getUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

}
