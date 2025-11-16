package com.dreammapper.service.impl;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dreammapper.dto.DreamComparisonDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamAnalysisRepository;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.service.ComparisonService;
import com.dreammapper.service.SimilarityService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComparisonServiceImpl implements ComparisonService {

	private final DreamRepository dreamRepository;
	private final DreamAnalysisRepository dreamAnalysisRepository;
	private final SimilarityService similarityService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public DreamComparisonDTO compareDreams(Dream dream1, Dream dream2, User user) {
		// Kullanıcı kontrolü
		if (!dream1.getUser().getId().equals(user.getId()) || 
				!dream2.getUser().getId().equals(user.getId())) {
			throw new IllegalArgumentException("User can only compare their own dreams");
		}

		// Genel benzerlik skoru
		double overallSimilarity = similarityService.calculateSimilarity(dream1, dream2);
		
		// Text benzerliği (sadece description)
		double textSimilarity = calculateTextSimilarity(dream1.getDescription(), dream2.getDescription());
		
		// Analizleri al
		DreamAnalysis analysis1 = getLatestAnalysis(dream1);
		DreamAnalysis analysis2 = getLatestAnalysis(dream2);
		
		// Kategori ve duygu karşılaştırması
		boolean sameCategory = analysis1 != null && analysis2 != null &&
				analysis1.getCategory() != null && analysis2.getCategory() != null &&
				analysis1.getCategory().equals(analysis2.getCategory());
		
		boolean sameEmotion = analysis1 != null && analysis2 != null &&
				analysis1.getDominantEmotion() != null && analysis2.getDominantEmotion() != null &&
				analysis1.getDominantEmotion().equals(analysis2.getDominantEmotion());
		
		// Ortak semboller
		List<String> commonSymbols = findCommonSymbols(analysis1, analysis2);
		
		// Duygusal skor farkları
		Map<String, Double> emotionScoreDifferences = calculateEmotionScoreDifferences(analysis1, analysis2);
		
		// Zaman farkı
		Long daysDifference = null;
		if (dream1.getCreatedAt() != null && dream2.getCreatedAt() != null) {
			daysDifference = Math.abs(ChronoUnit.DAYS.between(dream1.getCreatedAt(), dream2.getCreatedAt()));
		}
		
		// Benzerlik açıklaması
		String similarityDescription = generateSimilarityDescription(
				overallSimilarity, sameCategory, sameEmotion, commonSymbols.size(), daysDifference);
		
		return DreamComparisonDTO.builder()
				.dream1Id(dream1.getId())
				.dream2Id(dream2.getId())
				.overallSimilarity(Math.round(overallSimilarity * 100.0) / 100.0)
				.textSimilarity(Math.round(textSimilarity * 100.0) / 100.0)
				.sameCategory(sameCategory)
				.sameEmotion(sameEmotion)
				.commonSymbols(commonSymbols)
				.emotionScoreDifferences(emotionScoreDifferences)
				.daysDifference(daysDifference)
				.similarityDescription(similarityDescription)
				.build();
	}

	@Override
	public DreamComparisonDTO compareDreamsById(Long dream1Id, Long dream2Id, User user) {
		Dream dream1 = dreamRepository.findById(dream1Id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + dream1Id));
		Dream dream2 = dreamRepository.findById(dream2Id)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found with id: " + dream2Id));
		
		return compareDreams(dream1, dream2, user);
	}

	private double calculateTextSimilarity(String text1, String text2) {
		if (text1 == null || text2 == null) {
			return 0.0;
		}
		
		// Normalize
		String normalized1 = normalizeText(text1);
		String normalized2 = normalizeText(text2);
		
		// Jaccard similarity
		java.util.Set<String> words1 = java.util.Set.of(normalized1.split("\\s+"));
		java.util.Set<String> words2 = java.util.Set.of(normalized2.split("\\s+"));
		
		java.util.Set<String> intersection = new java.util.HashSet<>(words1);
		intersection.retainAll(words2);
		
		java.util.Set<String> union = new java.util.HashSet<>(words1);
		union.addAll(words2);
		
		if (union.isEmpty()) {
			return 0.0;
		}
		
		return (double) intersection.size() / union.size();
	}

	private String normalizeText(String text) {
		if (text == null) {
			return "";
		}
		return text.toLowerCase()
				.replaceAll("[^a-zçğıöşü\\s]", "")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private DreamAnalysis getLatestAnalysis(Dream dream) {
		List<DreamAnalysis> analyses = dreamAnalysisRepository.findByDreamOrderByCreatedAtDesc(dream);
		return analyses.isEmpty() ? null : analyses.get(0);
	}

	private List<String> findCommonSymbols(DreamAnalysis analysis1, DreamAnalysis analysis2) {
		if (analysis1 == null || analysis2 == null ||
				analysis1.getSymbolsText() == null || analysis2.getSymbolsText() == null) {
			return List.of();
		}
		
		List<String> symbols1 = java.util.Arrays.asList(analysis1.getSymbolsText().split(","))
				.stream()
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.collect(Collectors.toList());
		
		List<String> symbols2 = java.util.Arrays.asList(analysis2.getSymbolsText().split(","))
				.stream()
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.collect(Collectors.toList());
		
		return symbols1.stream()
				.filter(symbols2::contains)
				.distinct()
				.collect(Collectors.toList());
	}

	private Map<String, Double> calculateEmotionScoreDifferences(DreamAnalysis analysis1, DreamAnalysis analysis2) {
		Map<String, Double> differences = new HashMap<>();
		
		if (analysis1 == null || analysis2 == null) {
			return differences;
		}
		
		Map<String, Double> scores1 = parseScores(analysis1.getScoresJson());
		Map<String, Double> scores2 = parseScores(analysis2.getScoresJson());
		
		// Tüm duyguları birleştir
		java.util.Set<String> allEmotions = new java.util.HashSet<>();
		allEmotions.addAll(scores1.keySet());
		allEmotions.addAll(scores2.keySet());
		
		for (String emotion : allEmotions) {
			double score1 = scores1.getOrDefault(emotion, 0.0);
			double score2 = scores2.getOrDefault(emotion, 0.0);
			double diff = Math.abs(score1 - score2);
			if (diff > 0.01) { // Sadece önemli farkları göster
				differences.put(emotion, Math.round(diff * 100.0) / 100.0);
			}
		}
		
		return differences;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Double> parseScores(String scoresJson) {
		if (scoresJson == null || scoresJson.isBlank()) {
			return Map.of();
		}
		try {
			Map<String, Object> map = objectMapper.readValue(scoresJson, Map.class);
			Map<String, Double> result = new HashMap<>();
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				try {
					if (entry.getValue() instanceof Number) {
						result.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
					} else if (entry.getValue() != null) {
						result.put(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
					}
				} catch (Exception e) {
					// Skip invalid scores
				}
			}
			return result;
		} catch (Exception e) {
			return Map.of();
		}
	}

	private String generateSimilarityDescription(double similarity, boolean sameCategory, 
			boolean sameEmotion, int commonSymbolsCount, Long daysDifference) {
		List<String> descriptions = new ArrayList<>();
		
		if (similarity >= 0.8) {
			descriptions.add("Bu rüyalar çok benzer.");
		} else if (similarity >= 0.6) {
			descriptions.add("Bu rüyalar oldukça benzer.");
		} else if (similarity >= 0.4) {
			descriptions.add("Bu rüyalar orta düzeyde benzer.");
		} else {
			descriptions.add("Bu rüyalar birbirinden farklı.");
		}
		
		if (sameCategory) {
			descriptions.add("Aynı kategoride.");
		}
		
		if (sameEmotion) {
			descriptions.add("Aynı duyguyu taşıyor.");
		}
		
		if (commonSymbolsCount > 0) {
			descriptions.add(commonSymbolsCount + " ortak sembol bulunuyor.");
		}
		
		if (daysDifference != null && daysDifference > 0) {
			if (daysDifference < 7) {
				descriptions.add("Bir hafta içinde görülmüş.");
			} else if (daysDifference < 30) {
				descriptions.add("Bir ay içinde görülmüş.");
			} else {
				descriptions.add(daysDifference + " gün arayla görülmüş.");
			}
		}
		
		return String.join(" ", descriptions);
	}
}

