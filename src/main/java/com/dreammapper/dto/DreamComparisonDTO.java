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
public class DreamComparisonDTO {

	private Long dream1Id;
	private Long dream2Id;
	
	// Genel benzerlik skoru (0.0 - 1.0)
	private Double overallSimilarity;
	
	// Text benzerliği
	private Double textSimilarity;
	
	// Kategori benzerliği
	private Boolean sameCategory;
	
	// Duygu benzerliği
	private Boolean sameEmotion;
	
	// Ortak semboller
	private List<String> commonSymbols;
	
	// Duygusal skor karşılaştırması
	private Map<String, Double> emotionScoreDifferences;
	
	// Zaman farkı (gün cinsinden)
	private Long daysDifference;
	
	// Benzerlik açıklaması
	private String similarityDescription;
}

