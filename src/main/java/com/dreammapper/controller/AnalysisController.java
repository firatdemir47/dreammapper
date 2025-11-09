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
import com.dreammapper.model.User;
import com.dreammapper.service.AnalysisService;
import com.dreammapper.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisService analysisService;
    private final UserService userService;

	@PostMapping("/dream")
	public ResponseEntity<AnalysisResultDTO> analyze(@Valid @RequestBody AnalysisRequestDTO request,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Kullanıcı sadece kendi adına analiz yapabilir
		if (request.getUserId() != null && !request.getUserId().equals(currentUser.getId())) {
			return ResponseEntity.status(403).build();
		}

		// Eğer userId verilmemişse, current user'ı set et
		if (request.getUserId() == null) {
			request.setUserId(currentUser.getId());
		}

		// Dream kontrolü AnalysisService içinde yapılacak
		AnalysisResultDTO result = analysisService.analyzeDream(request);
		return ResponseEntity.ok(result);
	}

    @GetMapping("/dream/{dreamId}/history")
    public ResponseEntity<?> getDreamAnalysisHistory(@PathVariable Long dreamId,
			@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		try {
			List<AnalysisResultDTO> history = analysisService.getDreamAnalysisHistory(dreamId, currentUser.getId());
			return ResponseEntity.ok(history);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(403).build();
		}
    }
}
