package com.dreammapper.service;

import java.util.List;
import java.util.Optional;

import com.dreammapper.model.User;

public interface UserService {

	User saveUser(User user);

	List<User> getAllUsers();

	Optional<User> getUserById(Long id);

	void deleteUser(Long id);
}
