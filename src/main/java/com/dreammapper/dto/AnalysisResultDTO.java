package com.dreammapper.dto;

import java.util.List;
import java.util.Map;

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
public class AnalysisResultDTO {

	private String summary;

	private String dominantEmotion;

	private List<String> symbols;

	private Map<String, Double> scores;
	
	private String category;
}
