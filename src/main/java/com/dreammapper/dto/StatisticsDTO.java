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
public class StatisticsDTO {

	private Long totalDreams;
	private Long totalAnalyses;
	private Double averageDreamLength;
	
	// En çok görülen duygular (emotion -> count)
	private Map<String, Long> emotionDistribution;
	
	// En sık kullanılan tag'ler (tag -> count)
	private Map<String, Long> topTags;
	
	// Rüya kategorileri dağılımı (category -> count)
	private Map<String, Long> categoryDistribution;
	
	// Aylık trend (ay -> count)
	private Map<String, Long> monthlyTrend;
	
	// Haftalık trend (hafta -> count)
	private Map<String, Long> weeklyTrend;
	
	// En aktif günler (gün -> count)
	private Map<String, Long> dailyActivity;
	
	// Favori rüya sayısı
	private Long favoriteDreamsCount;
}

