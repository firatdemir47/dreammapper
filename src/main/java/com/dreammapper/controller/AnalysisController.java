package com.dreammapper.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dreammapper.dto.AnalysisRequestDTO;
import com.dreammapper.dto.AnalysisResultDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamAnalysisRepository;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.repository.UserRepository;
import com.dreammapper.service.AnalysisService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisService analysisService;
    private final DreamRepository dreamRepository;
    private final DreamAnalysisRepository dreamAnalysisRepository;
    private final UserRepository userRepository;

	@PostMapping("/dream")
	public ResponseEntity<AnalysisResultDTO> analyze(@Valid @RequestBody AnalysisRequestDTO request,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Eğer dreamId verilmişse, kullanıcının kendi rüyası olduğunu kontrol et
		if (request.getDreamId() != null) {
			Dream dream = dreamRepository.findById(request.getDreamId())
					.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));
			if (!dream.getUser().getId().equals(currentUser.getId())) {
				return ResponseEntity.status(403).build();
			}
		}

		// Kullanıcı sadece kendi adına analiz yapabilir
		if (request.getUserId() != null && !request.getUserId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		// Eğer userId verilmemişse, current user'ı set et
		if (request.getUserId() == null) {
			request.setUserId(currentUser.getId());
		}

		AnalysisResultDTO result = analysisService.analyzeDream(request);
		return ResponseEntity.ok(result);
	}

    @GetMapping("/dream/{dreamId}/history")
    public ResponseEntity<?> getDreamAnalysisHistory(@PathVariable Long dreamId,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userRepository.findByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Dream dream = dreamRepository.findById(dreamId)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı sadece kendi rüyasının analiz geçmişini görebilir
		if (!dream.getUser().getId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		return ResponseEntity.ok(toHistoryDto(dream));
    }

    private List<AnalysisResultDTO> toHistoryDto(Dream dream) {
        List<DreamAnalysis> list = dreamAnalysisRepository.findByDreamOrderByCreatedAtDesc(dream);
        return list.stream().map(a -> AnalysisResultDTO.builder()
                .summary(a.getSummary())
                .dominantEmotion(a.getDominantEmotion())
                .symbols(a.getSymbolsText() == null || a.getSymbolsText().isBlank() ? List.of() : java.util.Arrays.asList(a.getSymbolsText().split(",")))
                .scores(java.util.Collections.emptyMap())
                .build())
            .toList();
    }
}
