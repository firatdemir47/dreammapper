package com.dreammapper.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.mapper.DreamMapper;
import com.dreammapper.model.Dream;
import com.dreammapper.model.User;
import com.dreammapper.repository.UserRepository;
import com.dreammapper.service.DreamService;
import com.dreammapper.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dreams")
@RequiredArgsConstructor
public class DreamController {

	private final DreamService dreamService;
	private final UserService userService;
	private final UserRepository userRepository;

	@PostMapping
	public ResponseEntity<DreamDTO> saveDream(@RequestBody DreamDTO dreamDTO,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Kullanıcı sadece kendi adına rüya ekleyebilir
		if (dreamDTO.getUserId() != null && !dreamDTO.getUserId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		Dream dream = DreamMapper.toEntity(dreamDTO, currentUser);
		Dream savedDream = dreamService.saveDream(dream);
		return ResponseEntity.ok(DreamMapper.toDTO(savedDream));
	}

	@GetMapping
	public ResponseEntity<List<DreamDTO>> getMyDreams(@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		List<DreamDTO> dreams = dreamService.getDreamsByUser(currentUser).stream().map(DreamMapper::toDTO)
				.collect(Collectors.toList());
		return ResponseEntity.ok(dreams);
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<DreamDTO>> getDreamsByUser(@PathVariable Long userId,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Kullanıcı sadece kendi rüyalarını görebilir
		if (!userId.equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		return userService.getUserById(userId)
				.map(user -> dreamService.getDreamsByUser(user).stream().map(DreamMapper::toDTO)
						.collect(Collectors.toList()))
				.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/{id}")
	public ResponseEntity<DreamDTO> getDreamById(@PathVariable Long id,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return dreamService.getDreamById(id)
				.map(dream -> {
					// Kullanıcı sadece kendi rüyasını görebilir
					if (!dream.getUser().getId().equals(currentUser.getId())) {
						return null;
					}
					return DreamMapper.toDTO(dream);
				})
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.status(403).build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDream(@PathVariable Long id,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Dream dream = dreamService.getDreamById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı sadece kendi rüyasını silebilir
		if (!dream.getUser().getId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		dreamService.deleteDream(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public ResponseEntity<List<DreamDTO>> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String tags,
			@RequestParam(required = false) Boolean favorite,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Sadece kullanıcının kendi rüyalarında arama yap
		List<Dream> allDreams = dreamService.getDreamsByUser(currentUser);
		List<DreamDTO> results = allDreams.stream()
				.filter(dream -> {
					// Basit filtreleme
					if (q != null && !q.isBlank() && !dream.getDescription().toLowerCase().contains(q.toLowerCase())) {
						return false;
					}
					if (tags != null && !tags.isBlank()) {
						if (dream.getTagsText() == null || !dream.getTagsText().toLowerCase().contains(tags.toLowerCase())) {
							return false;
						}
					}
					if (favorite != null && !favorite.equals(dream.getFavorite())) {
						return false;
					}
					return true;
				})
				.map(DreamMapper::toDTO)
				.collect(Collectors.toList());
		return ResponseEntity.ok(results);
	}

	@PatchMapping("/{id}/favorite")
	public ResponseEntity<DreamDTO> setFavorite(@PathVariable Long id, @RequestParam boolean value,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Dream dream = dreamService.getDreamById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı sadece kendi rüyasını değiştirebilir
		if (!dream.getUser().getId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		dream.setFavorite(value);
		Dream saved = dreamService.saveDream(dream);
		return ResponseEntity.ok(DreamMapper.toDTO(saved));
	}

	@PatchMapping("/{id}/tags")
	public ResponseEntity<DreamDTO> setTags(@PathVariable Long id, @RequestParam String tags,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Dream dream = dreamService.getDreamById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı sadece kendi rüyasını değiştirebilir
		if (!dream.getUser().getId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		dream.setTagsText(tags);
		Dream saved = dreamService.saveDream(dream);
		return ResponseEntity.ok(DreamMapper.toDTO(saved));
	}
}
