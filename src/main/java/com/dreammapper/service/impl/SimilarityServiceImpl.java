package com.dreammapper.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dreammapper.dto.SimilarDreamDTO;
import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamAnalysisRepository;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.service.SimilarityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SimilarityServiceImpl implements SimilarityService {

	private final DreamRepository dreamRepository;
	private final DreamAnalysisRepository dreamAnalysisRepository;

	@Override
	public List<SimilarDreamDTO> findSimilarDreams(Dream dream, User user, int limit, double minSimilarity) {
		if (dream == null || user == null || dream.getDescription() == null) {
			return List.of();
		}

		List<Dream> userDreams = dreamRepository.findByUser(user);
		
		// Kendi rüyasını hariç tut
		userDreams = userDreams.stream()
				.filter(d -> !d.getId().equals(dream.getId()))
				.collect(Collectors.toList());

		List<SimilarDreamDTO> similarDreams = new ArrayList<>();
		
		for (Dream otherDream : userDreams) {
			double similarity = calculateSimilarity(dream, otherDream);
			if (similarity >= minSimilarity) {
				SimilarDreamDTO similar = buildSimilarDreamDTO(otherDream, similarity);
				similarDreams.add(similar);
			}
		}

		// Benzerlik skoruna göre sırala ve limit uygula
		return similarDreams.stream()
				.sorted(Comparator.comparing(SimilarDreamDTO::getSimilarityScore).reversed())
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Override
	public double calculateSimilarity(Dream dream1, Dream dream2) {
		if (dream1 == null || dream2 == null || 
				dream1.getDescription() == null || dream2.getDescription() == null) {
			return 0.0;
		}

		String text1 = normalizeText(dream1.getDescription());
		String text2 = normalizeText(dream2.getDescription());

		// Jaccard Similarity kullan (kelime bazlı)
		double jaccardSimilarity = calculateJaccardSimilarity(text1, text2);
		
		// Levenshtein distance benzerliği (karakter bazlı)
		double levenshteinSimilarity = calculateLevenshteinSimilarity(text1, text2);
		
		// Kategori ve duygu benzerliği
		double categoryEmotionSimilarity = calculateCategoryEmotionSimilarity(dream1, dream2);
		
		// Ağırlıklı ortalama
		// Jaccard: 50%, Levenshtein: 30%, Category/Emotion: 20%
		return (jaccardSimilarity * 0.5) + (levenshteinSimilarity * 0.3) + (categoryEmotionSimilarity * 0.2);
	}

	@Override
	public List<List<SimilarDreamDTO>> findRecurringDreams(User user, double minSimilarity) {
		List<Dream> userDreams = dreamRepository.findByUser(user);
		List<List<SimilarDreamDTO>> recurringGroups = new ArrayList<>();
		Set<Long> processed = new HashSet<>();

		for (Dream dream : userDreams) {
			if (processed.contains(dream.getId())) {
				continue;
			}

			List<SimilarDreamDTO> group = new ArrayList<>();
			group.add(buildSimilarDreamDTO(dream, 1.0)); // Kendisi
			processed.add(dream.getId());

			for (Dream otherDream : userDreams) {
				if (processed.contains(otherDream.getId()) || dream.getId().equals(otherDream.getId())) {
					continue;
				}

				double similarity = calculateSimilarity(dream, otherDream);
				if (similarity >= minSimilarity) {
					group.add(buildSimilarDreamDTO(otherDream, similarity));
					processed.add(otherDream.getId());
				}
			}

			if (group.size() > 1) { // En az 2 rüya olmalı
				recurringGroups.add(group);
			}
		}

		return recurringGroups;
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

	private double calculateJaccardSimilarity(String text1, String text2) {
		if (text1.isBlank() || text2.isBlank()) {
			return 0.0;
		}

		Set<String> words1 = Set.of(text1.split("\\s+"));
		Set<String> words2 = Set.of(text2.split("\\s+"));

		Set<String> intersection = new HashSet<>(words1);
		intersection.retainAll(words2);

		Set<String> union = new HashSet<>(words1);
		union.addAll(words2);

		if (union.isEmpty()) {
			return 0.0;
		}

		return (double) intersection.size() / union.size();
	}

	private double calculateLevenshteinSimilarity(String text1, String text2) {
		if (text1.equals(text2)) {
			return 1.0;
		}
		if (text1.length() == 0 || text2.length() == 0) {
			return 0.0;
		}

		int maxLength = Math.max(text1.length(), text2.length());
		int distance = levenshteinDistance(text1, text2);
		
		return 1.0 - ((double) distance / maxLength);
	}

	private int levenshteinDistance(String s1, String s2) {
		int[][] dp = new int[s1.length() + 1][s2.length() + 1];

		for (int i = 0; i <= s1.length(); i++) {
			dp[i][0] = i;
		}
		for (int j = 0; j <= s2.length(); j++) {
			dp[0][j] = j;
		}

		for (int i = 1; i <= s1.length(); i++) {
			for (int j = 1; j <= s2.length(); j++) {
				if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
					dp[i][j] = dp[i - 1][j - 1];
				} else {
					dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
				}
			}
		}

		return dp[s1.length()][s2.length()];
	}

	private double calculateCategoryEmotionSimilarity(Dream dream1, Dream dream2) {
		// Her iki rüyanın da analizini al
		DreamAnalysis analysis1 = getLatestAnalysis(dream1);
		DreamAnalysis analysis2 = getLatestAnalysis(dream2);

		if (analysis1 == null || analysis2 == null) {
			return 0.0;
		}

		double score = 0.0;
		int factors = 0;

		// Kategori benzerliği
		if (analysis1.getCategory() != null && analysis2.getCategory() != null) {
			if (analysis1.getCategory().equals(analysis2.getCategory())) {
				score += 1.0;
			}
			factors++;
		}

		// Duygu benzerliği
		if (analysis1.getDominantEmotion() != null && analysis2.getDominantEmotion() != null) {
			if (analysis1.getDominantEmotion().equals(analysis2.getDominantEmotion())) {
				score += 1.0;
			}
			factors++;
		}

		return factors > 0 ? score / factors : 0.0;
	}

	private DreamAnalysis getLatestAnalysis(Dream dream) {
		List<DreamAnalysis> analyses = dreamAnalysisRepository.findByDreamOrderByCreatedAtDesc(dream);
		return analyses.isEmpty() ? null : analyses.get(0);
	}

	private SimilarDreamDTO buildSimilarDreamDTO(Dream dream, double similarity) {
		DreamAnalysis latestAnalysis = getLatestAnalysis(dream);
		
		return SimilarDreamDTO.builder()
				.dreamId(dream.getId())
				.description(dream.getDescription().length() > 200 
						? dream.getDescription().substring(0, 200) + "..." 
						: dream.getDescription())
				.createdAt(dream.getCreatedAt())
				.category(latestAnalysis != null ? latestAnalysis.getCategory() : null)
				.dominantEmotion(latestAnalysis != null ? latestAnalysis.getDominantEmotion() : null)
				.similarityScore(Math.round(similarity * 100.0) / 100.0) // 2 decimal
				.build();
	}
}

