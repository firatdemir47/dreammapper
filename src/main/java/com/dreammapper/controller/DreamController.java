package com.dreammapper.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dreammapper.model.Dream;
import com.dreammapper.service.DreamService;
import com.dreammapper.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dreams")
@RequiredArgsConstructor
public class DreamController {

	private final DreamService dreamService;
	private final UserService userService;

	@PostMapping
	public ResponseEntity<Dream> saveDream(@RequestBody Dream dream) {
		Dream savedDream = dreamService.saveDream(dream);
		return ResponseEntity.ok(savedDream);
	}

	@GetMapping
	public ResponseEntity<List<Dream>> getAllDreams() {
		return ResponseEntity.ok(dreamService.getAllDreams());
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Dream>> getDreamsByUser(@PathVariable Long userId) {
		return userService.getUserById(userId).map(user -> ResponseEntity.ok(dreamService.getDreamsByUser(user)))
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Dream> getDreamById(@PathVariable Long id) {
		return dreamService.getDreamById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDream(@PathVariable Long id) {
		dreamService.deleteDream(id);
		return ResponseEntity.noContent().build();
	}
}
