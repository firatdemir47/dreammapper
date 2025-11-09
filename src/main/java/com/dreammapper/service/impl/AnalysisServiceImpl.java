package com.dreammapper.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.dreammapper.dto.AnalysisRequestDTO;
import com.dreammapper.dto.AnalysisResultDTO;
import com.dreammapper.exception.ResourceNotFoundException;
import com.dreammapper.model.Dream;
import com.dreammapper.model.DreamAnalysis;
import com.dreammapper.model.User;
import com.dreammapper.repository.DreamAnalysisRepository;
import com.dreammapper.repository.DreamRepository;
import com.dreammapper.repository.UserRepository;
import com.dreammapper.service.AnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {
	@Value("${gemini.api.key}")
	private String geminiApiKey;

	private static final Logger log = LoggerFactory.getLogger(AnalysisServiceImpl.class);
    private final RestTemplate restTemplate;
    private final DreamRepository dreamRepository;
    private final DreamAnalysisRepository dreamAnalysisRepository;
    private final UserRepository userRepository;

	@Override
	public AnalysisResultDTO analyzeDream(AnalysisRequestDTO request) {
		// Eğer dreamId verilmişse, kullanıcının kendi rüyası olduğunu kontrol et
		if (request.getDreamId() != null && request.getUserId() != null) {
			Dream dream = dreamRepository.findById(request.getDreamId())
					.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));
			if (!dream.getUser().getId().equals(request.getUserId())) {
				throw new IllegalArgumentException("User does not have access to this dream");
			}
		}

        String text = preprocess(Optional.ofNullable(request.getText()).orElse(""));
        if (text.isBlank()) {
            return AnalysisResultDTO.builder()
                .summary("Boş veya geçersiz metin gönderildi.")
                .dominantEmotion("nötr")
                .symbols(List.of())
                .scores(Map.of())
                .build();
        }

		try {
            String aiResponse = callGemini(text);
            AnalysisResultDTO result = AnalysisResultDTO.builder()
                .summary(aiResponse)
                .dominantEmotion("AI Analizi")
                .symbols(List.of())
                .scores(Map.of())
                .build();

            // Persist analysis linked to a Dream if we can resolve one
            persistAnalysisIfPossible(request, text, result);
            return result;
		} catch (Exception e) {
			return AnalysisResultDTO.builder().summary("Rüya analizi alınamadı, lütfen daha sonra tekrar deneyin.")
					.dominantEmotion("nötr").symbols(List.of()).scores(Map.of()).build();
		}
	}

	private String callGemini(String text) {
		final String modelId = "gemini-2.5-flash-preview-05-20";
		final String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId
				+ ":generateContent";
		final String url = baseUrl + "?key=" + geminiApiKey;
		log.info("Calling Gemini endpoint: {}", baseUrl);

		String requestBody = """
				{
				  "contents": [
				    {
				      "parts": [
				        {"text": "Bir rüya analisti gibi davran. Aşağıdaki rüyayı kısa ve sade Türkçe ile, Markdown kullanmadan özetle ve yorumla. Metinde sadece düz yazı kullan: %s"}
				      ]
				    }
				  ]
				}
				"""
				.formatted(text);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
				Map<?, ?> response = resp.getBody();
				if (response == null || !response.containsKey("candidates")) {
					log.warn("Gemini empty or no 'candidates'. Raw response: {}", response);
					return "Yanıt alınamadı.";
				}
				var candidates = (List<Map<String, Object>>) response.get("candidates");
				if (candidates.isEmpty())
					return "Analiz bulunamadı.";
				var content = (Map<String, Object>) candidates.get(0).get("content");
				if (content == null)
					return "Yanıt alınamadı.";
				var parts = (List<Map<String, Object>>) content.get("parts");
				if (parts == null || parts.isEmpty())
					return "Yanıt alınamadı.";
				Object firstPartText = parts.get(0).get("text");
				String raw = firstPartText == null ? "Yanıt alınamadı." : firstPartText.toString();
				return cleanPlainText(raw);
			} catch (HttpClientErrorException httpEx) {
				String body = httpEx.getResponseBodyAsString();
				if (httpEx.getStatusCode().value() == 429 && attempt == 1) {
					continue;
				}
				log.error("Gemini HTTP {} - {}", httpEx.getStatusCode().value(), httpEx.getStatusText());
				log.error("Gemini response: {}", body);
				return body != null && !body.isBlank() ? ("Gemini hata yanıtı: " + body) : "Yanıt alınamadı.";
			} catch (RestClientResponseException rce) {
				String body = rce.getResponseBodyAsString();
				log.error("Gemini error: status={} body={}", rce.getRawStatusCode(), body);
				return body != null && !body.isBlank() ? ("Gemini hata yanıtı: " + body) : "Yanıt alınamadı.";
			}
		}
		return "Yanıt alınamadı.";
	}

	private String cleanPlainText(String text) {
		if (text == null)
			return "";
		String cleaned = text.replace("**", "").replace("---", "").replace("\r", "").replace("\t", " ")
				.replace("* ", "- ").replace("• ", "- ");
		cleaned = cleaned.replaceAll("\n{3,}", "\n\n").trim();
		return cleaned;
	}

    private String preprocess(String raw) {
        if (raw == null) return "";
        String text = raw.strip();
        // Collapse whitespace
        text = text.replaceAll("\u00A0", " ");
        text = text.replaceAll("[\n\r\t]+", " ");
        text = text.replaceAll(" {2,}", " ").trim();
        // Basic spam check: long repeated characters
        if (Pattern.compile("(.)\\1{9,}").matcher(text).find()) {
            return "";
        }
        // Cap length (e.g., 4000 chars)
        int max = 4000;
        if (text.length() > max) {
            text = text.substring(0, max);
        }
        return text;
    }

    private void persistAnalysisIfPossible(AnalysisRequestDTO request, String normalizedText, AnalysisResultDTO result) {
        try {
            Dream targetDream = null;
            if (request.getDreamId() != null) {
                targetDream = dreamRepository.findById(request.getDreamId()).orElse(null);
            }
            if (targetDream == null && request.getUserId() != null) {
                User owner = userRepository.findById(request.getUserId()).orElse(null);
                if (owner != null) {
                    Dream newDream = Dream.builder()
                        .description(normalizedText)
                        .user(owner)
                        .build();
                    targetDream = dreamRepository.save(newDream);
                }
            }
            if (targetDream == null) {
                return;
            }
            DreamAnalysis analysis = DreamAnalysis.builder()
                .dream(targetDream)
                .summary(Optional.ofNullable(result.getSummary()).orElse(""))
                .dominantEmotion(result.getDominantEmotion())
                .symbolsText(String.join(",", Optional.ofNullable(result.getSymbols()).orElse(List.of())))
                .scoresJson(mapToJson(result.getScores()))
                .build();
            dreamAnalysisRepository.save(analysis);
        } catch (Exception ex) {
            log.warn("Persisting analysis failed: {}", ex.getMessage());
        }
    }

    private String mapToJson(Map<String, Double> scores) {
        if (scores == null || scores.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Double> e : scores.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(e.getKey().replace("\"", "\\\"")).append('"')
              .append(':')
              .append(e.getValue() == null ? "null" : e.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

	@Override
	public List<AnalysisResultDTO> getDreamAnalysisHistory(Long dreamId, Long userId) {
		Dream dream = dreamRepository.findById(dreamId)
				.orElseThrow(() -> new ResourceNotFoundException("Dream not found"));

		// Kullanıcı kontrolü
		if (!dream.getUser().getId().equals(userId)) {
			throw new IllegalArgumentException("User does not have access to this dream");
		}

		List<DreamAnalysis> analyses = dreamAnalysisRepository.findByDreamOrderByCreatedAtDesc(dream);
		return analyses.stream()
				.map(a -> AnalysisResultDTO.builder()
						.summary(a.getSummary())
						.dominantEmotion(a.getDominantEmotion())
						.symbols(a.getSymbolsText() == null || a.getSymbolsText().isBlank() 
								? List.of() 
								: java.util.Arrays.asList(a.getSymbolsText().split(",")))
						.scores(java.util.Collections.emptyMap())
						.build())
				.toList();
	}
}
