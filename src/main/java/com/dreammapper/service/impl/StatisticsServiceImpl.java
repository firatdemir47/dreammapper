package com.dreammapper.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.dreammapper.dto.StatisticsDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamAnalysisRepository;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.repository.UserRepository;
import com.dreammapper.service.StatisticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

	private final DreamRepository dreamRepository;
	private final DreamAnalysisRepository dreamAnalysisRepository;
	private final UserRepository userRepository;

	@Override
	public StatisticsDTO getUserStatistics(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
		return getUserStatistics(user);
	}

	@Override
	public StatisticsDTO getUserStatistics(User user) {
		List<Dream> dreams = dreamRepository.findByUser(user);
		List<DreamAnalysis> analyses = dreamAnalysisRepository.findByUser(user);

		StatisticsDTO.StatisticsDTOBuilder builder = StatisticsDTO.builder()
				.totalDreams((long) dreams.size())
				.totalAnalyses((long) analyses.size())
				.averageDreamLength(calculateAverageDreamLength(dreams))
				.favoriteDreamsCount(dreams.stream().filter(d -> Boolean.TRUE.equals(d.getFavorite())).count())
				.emotionDistribution(calculateEmotionDistribution(analyses))
				.categoryDistribution(calculateCategoryDistribution(analyses))
				.topTags(calculateTopTags(dreams))
				.monthlyTrend(calculateMonthlyTrend(dreams))
				.weeklyTrend(calculateWeeklyTrend(dreams))
				.dailyActivity(calculateDailyActivity(dreams));

		return builder.build();
	}

	private Double calculateAverageDreamLength(List<Dream> dreams) {
		if (dreams.isEmpty()) {
			return 0.0;
		}
		double totalLength = dreams.stream()
				.mapToInt(d -> d.getDescription() != null ? d.getDescription().length() : 0)
				.sum();
		return totalLength / dreams.size();
	}

	private Map<String, Long> calculateEmotionDistribution(List<DreamAnalysis> analyses) {
		return analyses.stream()
				.filter(a -> a.getDominantEmotion() != null && !a.getDominantEmotion().isBlank())
				.collect(Collectors.groupingBy(
						DreamAnalysis::getDominantEmotion,
						Collectors.counting()));
	}

	private Map<String, Long> calculateCategoryDistribution(List<DreamAnalysis> analyses) {
		return analyses.stream()
				.filter(a -> a.getCategory() != null && !a.getCategory().isBlank())
				.collect(Collectors.groupingBy(
						DreamAnalysis::getCategory,
						Collectors.counting()));
	}

	private Map<String, Long> calculateTopTags(List<Dream> dreams) {
		Map<String, Long> tagCounts = new HashMap<>();
		for (Dream dream : dreams) {
			if (dream.getTagsText() != null && !dream.getTagsText().isBlank()) {
				String[] tags = dream.getTagsText().split(",");
				for (String tag : tags) {
					String trimmedTag = tag.trim();
					if (!trimmedTag.isBlank()) {
						tagCounts.put(trimmedTag, tagCounts.getOrDefault(trimmedTag, 0L) + 1);
					}
				}
			}
		}
		// En çok kullanılan 10 tag'i döndür
		return tagCounts.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.limit(10)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private Map<String, Long> calculateMonthlyTrend(List<Dream> dreams) {
		Map<String, Long> monthlyCounts = new HashMap<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
		
		for (Dream dream : dreams) {
			if (dream.getCreatedAt() != null) {
				String month = dream.getCreatedAt().format(formatter);
				monthlyCounts.put(month, monthlyCounts.getOrDefault(month, 0L) + 1);
			}
		}
		
		// Son 6 ayı döndür
		return monthlyCounts.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByKey().reversed())
				.limit(6)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private Map<String, Long> calculateWeeklyTrend(List<Dream> dreams) {
		Map<String, Long> weeklyCounts = new HashMap<>();
		
		for (Dream dream : dreams) {
			if (dream.getCreatedAt() != null) {
				LocalDateTime createdAt = dream.getCreatedAt();
				int year = createdAt.getYear();
				int week = createdAt.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
				String weekKey = String.format("%d-W%02d", year, week);
				weeklyCounts.put(weekKey, weeklyCounts.getOrDefault(weekKey, 0L) + 1);
			}
		}
		
		// Son 12 haftayı döndür
		return weeklyCounts.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByKey().reversed())
				.limit(12)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private Map<String, Long> calculateDailyActivity(List<Dream> dreams) {
		Map<String, Long> dailyCounts = new HashMap<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		for (Dream dream : dreams) {
			if (dream.getCreatedAt() != null) {
				String date = dream.getCreatedAt().format(formatter);
				dailyCounts.put(date, dailyCounts.getOrDefault(date, 0L) + 1);
			}
		}
		
		// Son 30 günü döndür
		return dailyCounts.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByKey().reversed())
				.limit(30)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}

