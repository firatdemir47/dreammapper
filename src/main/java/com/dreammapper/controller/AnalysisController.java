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
import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.repository.DreamAnalysisRepository;
import com.dreammapper.repository.DreamRepository;
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

	@PostMapping("/dream")
	public ResponseEntity<AnalysisResultDTO> analyze(@Valid @RequestBody AnalysisRequestDTO request,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal != null && request.getUserId() == null) {
			// optionally set user context in future
		}
		AnalysisResultDTO result = analysisService.analyzeDream(request);
		return ResponseEntity.ok(result);
	}

    @GetMapping("/dream/{dreamId}/history")
    public ResponseEntity<?> getDreamAnalysisHistory(@PathVariable Long dreamId) {
        return dreamRepository.findById(dreamId)
            .<ResponseEntity<?>>map(dream -> ResponseEntity.ok(toHistoryDto(dream)))
            .orElseGet(() -> ResponseEntity.notFound().build());
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
