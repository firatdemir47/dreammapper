package com.dreammapper.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimilarDreamDTO {

	private Long dreamId;
	private String description;
	private LocalDateTime createdAt;
	private String category;
	private String dominantEmotion;
	private Double similarityScore; // 0.0 - 1.0 arası benzerlik skoru
}

