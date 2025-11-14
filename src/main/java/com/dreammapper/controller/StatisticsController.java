package com.dreammapper.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dreammapper.dto.StatisticsDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.model.User;
import com.dreammapper.service.StatisticsService;
import com.dreammapper.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

	private final StatisticsService statisticsService;
	private final UserService userService;

	@GetMapping
	public ResponseEntity<StatisticsDTO> getMyStatistics(@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			return ResponseEntity.status(401).build();
		}

		User currentUser = userService.getUserByEmail(principal.getUsername())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		StatisticsDTO statistics = statisticsService.getUserStatistics(currentUser);
		return ResponseEntity.ok(statistics);
	}
}

