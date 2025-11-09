package com.dreammapper.service;

import java.util.List;
import java.util.Optional;

import com.dreammapper.dto.ChangePasswordDTO;
import com.dreammapper.dto.UpdateProfileDTO;
import com.dreammapper.model.User;

public interface UserService {

	User saveUser(User user);

	List<User> getAllUsers();

	Optional<User> getUserById(Long id);

	void deleteUser(Long id);

	User updateProfile(Long userId, UpdateProfileDTO dto);

	void changePassword(Long userId, ChangePasswordDTO dto);

	Optional<User> getUserByEmail(String email);
}
