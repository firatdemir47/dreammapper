package com.dreammapper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dreammapper.dto.AnalysisRequestDTO;
import com.dreammapper.dto.AnalysisResultDTO;
import com.dreammapper.service.AnalysisService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

	private final AnalysisService analysisService;

	@PostMapping("/dream")
	public ResponseEntity<AnalysisResultDTO> analyze(@Valid @RequestBody AnalysisRequestDTO request) {
		AnalysisResultDTO result = analysisService.analyzeDream(request);
		return ResponseEntity.ok(result);
	}
}
