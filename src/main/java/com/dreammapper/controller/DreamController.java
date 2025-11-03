package com.dreammapper.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dreammapper.dto.DreamDTO;
import com.dreammapper.mapper.DreamMapper;
import com.dreammapper.model.Dream;
import com.dreammapper.model.User;
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
	public ResponseEntity<DreamDTO> saveDream(@RequestBody DreamDTO dreamDTO) {
		User user = userService.getUserById(dreamDTO.getUserId())
				.orElseThrow(() -> new RuntimeException("User not found"));
		Dream dream = DreamMapper.toEntity(dreamDTO, user);
		Dream savedDream = dreamService.saveDream(dream);
		return ResponseEntity.ok(DreamMapper.toDTO(savedDream));

	}

	@GetMapping
	public ResponseEntity<List<DreamDTO>> getAllDreams() {
		List<DreamDTO> dreams = dreamService.getAllDreams().stream().map(DreamMapper::toDTO)
				.collect(Collectors.toList());
		return ResponseEntity.ok(dreams);
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<DreamDTO>> getDreamsByUser(@PathVariable Long userId) {
		return userService.getUserById(userId)
				.map(user -> dreamService.getDreamsByUser(user).stream().map(DreamMapper::toDTO)
						.collect(Collectors.toList()))
				.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/{id}")
	public ResponseEntity<DreamDTO> getDreamById(@PathVariable Long id) {
		return dreamService.getDreamById(id).map(DreamMapper::toDTO).map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDream(@PathVariable Long id) {
		dreamService.deleteDream(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public ResponseEntity<List<DreamDTO>> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String tags,
			@RequestParam(required = false) Boolean favorite) {
		List<DreamDTO> results = dreamService.search(q, tags, favorite).stream().map(DreamMapper::toDTO)
				.collect(Collectors.toList());
		return ResponseEntity.ok(results);
	}

	@PatchMapping("/{id}/favorite")
	public ResponseEntity<DreamDTO> setFavorite(@PathVariable Long id, @RequestParam boolean value) {
		var dreamOpt = dreamService.getDreamById(id);
		if (dreamOpt.isEmpty()) return ResponseEntity.notFound().build();
		var d = dreamOpt.get();
		d.setFavorite(value);
		var saved = dreamService.saveDream(d);
		return ResponseEntity.ok(DreamMapper.toDTO(saved));
	}

	@PatchMapping("/{id}/tags")
	public ResponseEntity<DreamDTO> setTags(@PathVariable Long id, @RequestParam String tags) {
		var dreamOpt = dreamService.getDreamById(id);
		if (dreamOpt.isEmpty()) return ResponseEntity.notFound().build();
		var d = dreamOpt.get();
		d.setTagsText(tags);
		var saved = dreamService.saveDream(d);
		return ResponseEntity.ok(DreamMapper.toDTO(saved));
	}
}
