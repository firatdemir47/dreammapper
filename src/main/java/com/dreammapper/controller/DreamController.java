package com.dreammapper.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import com.dreammapper.dto.DreamDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.mapper.DreamMapper;
import com.dreammapper.model.Dream;
import com.dreammapper.model.User;
import com.dreammapper.dto.DreamComparisonDTO;
import com.dreammapper.dto.SimilarDreamDTO;
import com.dreammapper.service.ComparisonService;
import com.dreammapper.service.DreamService;
import com.dreammapper.service.ExportService;
import com.dreammapper.service.SimilarityService;
import com.dreammapper.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dreams")
@RequiredArgsConstructor
public class DreamController {

	private final DreamService dreamService;
	private final UserService userService;
	private final SimilarityService similarityService;
	private final ComparisonService comparisonService;
	private final ExportService exportService;
	private final ImportService importService;
	private final ExportService exportService;

	@PostMapping
	public ResponseEntity<DreamDTO> saveDream(@RequestBody DreamDTO dreamDTO,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
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
	public ResponseEntity<?> getMyDreams(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Pageable pageable = PageRequest.of(page, size);
		Page<Dream> dreamsPage = dreamService.getDreamsByUser(currentUser, pageable);
		Page<DreamDTO> dreamsDtoPage = dreamsPage.map(DreamMapper::toDTO);
		return ResponseEntity.ok(dreamsDtoPage);
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<DreamDTO>> getDreamsByUser(@PathVariable Long userId,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
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

		User currentUser = userService.getUserByEmail(principal.getUsername())
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

		User currentUser = userService.getUserByEmail(principal.getUsername())
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

		User currentUser = userService.getUserByEmail(principal.getUsername())
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

		User currentUser = userService.getUserByEmail(principal.getUsername())
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

		User currentUser = userService.getUserByEmail(principal.getUsername())
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

	@PatchMapping("/{id}/notes")
	public ResponseEntity<DreamDTO> updateNotes(@PathVariable Long id, @RequestParam String notes,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Dream dream = dreamService.getDreamById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı sadece kendi rüyasını değiştirebilir
		if (!dream.getUser().getId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		dream.setNotes(notes);
		Dream saved = dreamService.saveDream(dream);
		return ResponseEntity.ok(DreamMapper.toDTO(saved));
	}

	@PutMapping("/{id}")
	public ResponseEntity<DreamDTO> updateDream(@PathVariable Long id, @Valid @RequestBody DreamDTO dreamDTO,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Dream existingDream = dreamService.getDreamById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı sadece kendi rüyasını güncelleyebilir
		if (!existingDream.getUser().getId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		// Kullanıcı sadece kendi adına rüya güncelleyebilir
		if (dreamDTO.getUserId() != null && !dreamDTO.getUserId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		// Rüyayı güncelle
		existingDream.setDescription(dreamDTO.getDescription());
		existingDream.setMood(dreamDTO.getMood());
		existingDream.setTagsText(dreamDTO.getTagsText());
		existingDream.setFavorite(dreamDTO.getFavorite() != null ? dreamDTO.getFavorite() : false);
		existingDream.setNotes(dreamDTO.getNotes());

		Dream saved = dreamService.saveDream(existingDream);
		return ResponseEntity.ok(DreamMapper.toDTO(saved));
	}

	@GetMapping("/{id}/similar")
	public ResponseEntity<List<SimilarDreamDTO>> getSimilarDreams(
			@PathVariable Long id,
			@RequestParam(defaultValue = "5") int limit,
			@RequestParam(defaultValue = "0.5") double minSimilarity,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Dream dream = dreamService.getDreamById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı sadece kendi rüyasını sorgulayabilir
		if (!dream.getUser().getId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		List<SimilarDreamDTO> similarDreams = similarityService.findSimilarDreams(
				dream, currentUser, limit, minSimilarity);
		return ResponseEntity.ok(similarDreams);
	}

	@GetMapping("/recurring")
	public ResponseEntity<List<List<SimilarDreamDTO>>> getRecurringDreams(
			@RequestParam(defaultValue = "0.7") double minSimilarity,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		List<List<SimilarDreamDTO>> recurringDreams = similarityService.findRecurringDreams(
				currentUser, minSimilarity);
		return ResponseEntity.ok(recurringDreams);
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> exportDreams(
			@RequestParam(defaultValue = "json") String format,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		try {
			ExportService.ExportResult result = exportService.exportDreams(currentUser, format);
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"");
			headers.add(HttpHeaders.CONTENT_TYPE, result.contentType());
			return ResponseEntity.ok()
					.headers(headers)
					.body(result.content());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/compare")
	public ResponseEntity<DreamComparisonDTO> compareDreams(
			@RequestParam Long dream1Id,
			@RequestParam Long dream2Id,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		try {
			DreamComparisonDTO comparison = comparisonService.compareDreamsById(
					dream1Id, dream2Id, currentUser);
			return ResponseEntity.ok(comparison);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(403).body(null);
		}
	}

	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importDreams(
			@RequestBody String jsonData,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		try {
			ImportService.ImportResult result = importService.importDreamsFromJson(jsonData, currentUser);
			Map<String, Object> response = Map.of(
					"successCount", result.successCount(),
					"errorCount", result.errorCount(),
					"errors", result.errors()
			);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "Import hatası: " + e.getMessage()));
		}
	}
}
