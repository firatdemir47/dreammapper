package com.dreammapper.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.dreammapper.dto.AnalysisRequestDTO;
import com.dreammapper.dto.AnalysisResultDTO;
import com.dreammapper.service.AnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

	// TODO: OpenAI entegrasyonu eklerken buraya WebClient/OpenAiService enjekte
	// edilecek.

	@Override
	public AnalysisResultDTO analyzeDream(AnalysisRequestDTO request) {
		String text = Optional.ofNullable(request.getText()).orElse("").toLowerCase(Locale.ROOT);

		List<String> symbols = new ArrayList<>();
		if (text.contains("su"))
			symbols.add("su");
		if (text.contains("uç") || text.contains("uçuyordum"))
			symbols.add("uçmak");
		if (text.contains("karanlık"))
			symbols.add("karanlık");
		if (text.contains("köpek"))
			symbols.add("köpek");

		String dominantEmotion = "nötr";
		if (text.contains("korku") || text.contains("korktum") || text.contains("karanlık"))
			dominantEmotion = "korku";
		else if (text.contains("mutlu") || text.contains("huzur"))
			dominantEmotion = "huzur";
		else if (text.contains("öfke"))
			dominantEmotion = "öfke";
		else if (text.contains("özgür"))
			dominantEmotion = "özgürlük";

		Map<String, Double> scores = new LinkedHashMap<>();
		scores.put("korku", dominantEmotion.equals("korku") ? 0.8 : 0.1);
		scores.put("huzur", dominantEmotion.equals("huzur") ? 0.75 : 0.15);
		scores.put("özgürlük", dominantEmotion.equals("özgürlük") ? 0.7 : 0.2);

		String summary = buildSummary(symbols, dominantEmotion);

		return AnalysisResultDTO.builder().summary(summary).dominantEmotion(dominantEmotion).symbols(symbols)
				.scores(scores).build();
	}

	private String buildSummary(List<String> symbols, String emotion) {
		String sym = symbols.isEmpty() ? "belirgin sembol tespit edilmedi" : String.join(", ", symbols);
		return "Rüyada " + sym + " temaları gözleniyor. Baskın duygu: " + emotion + ".";
	}
}
