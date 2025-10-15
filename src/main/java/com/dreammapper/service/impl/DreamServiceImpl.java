package com.dreammapper.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.dreammapper.model.Dream;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.service.DreamService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DreamServiceImpl implements DreamService {

	private final DreamRepository dreamRepository;

	@Override
	public Dream saveDream(Dream dream) {

		return dreamRepository.save(dream);
	}

	@Override
	public List<Dream> getDreamsByUser(User user) {

		return dreamRepository.findByUser(user);
	}

	@Override
	public List<Dream> getAllDreams() {

		return dreamRepository.findAll();
	}

	@Override
	public Optional<Dream> getDreamById(Long id) {
		return dreamRepository.findById(id);
	}

	@Override
	public void deleteDream(Long id) {
		dreamRepository.deleteById(id);

	}

}
