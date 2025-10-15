package com.dreammapper.service;

import java.util.List;
import java.util.Optional;

import com.dreammapper.model.Dream;
import com.dreammapper.model.User;

public interface DreamService {

	Dream saveDream(Dream dream);

	List<Dream> getDreamsByUser(User user);

	List<Dream> getAllDreams();

	Optional<Dream> getDreamById(Long id);

	void deleteDream(Long id);
}
