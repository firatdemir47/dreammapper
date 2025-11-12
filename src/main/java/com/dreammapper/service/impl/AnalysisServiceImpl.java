package com.dreammapper.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            AnalysisResultDTO result = callGeminiForAnalysis(text);
            
            // Persist analysis linked to a Dream if we can resolve one
            persistAnalysisIfPossible(request, text, result);
            return result;
		} catch (Exception e) {
			log.error("Error analyzing dream: ", e);
			return AnalysisResultDTO.builder()
					.summary("Rüya analizi alınamadı, lütfen daha sonra tekrar deneyin.")
					.dominantEmotion("nötr")
					.symbols(List.of())
					.scores(Map.of())
					.category("bilinmeyen")
					.build();
		}
	}

	private AnalysisResultDTO callGeminiForAnalysis(String text) {
		final String modelId = "gemini-2.5-flash-preview-05-20";
		final String baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelId
				+ ":generateContent";
		final String url = baseUrl + "?key=" + geminiApiKey;
		log.info("Calling Gemini endpoint: {}", baseUrl);

		String prompt = String.format(
				"Bir rüya analisti gibi davran. Aşağıdaki rüyayı analiz et ve sonucu SADECE JSON formatında döndür. " +
				"JSON formatı şu şekilde olmalı: " +
				"{\"summary\": \"Rüyanın kısa ve özet analizi (Türkçe, düz metin, Markdown yok)\", " +
				"\"dominantEmotion\": \"En baskın duygu (mutluluk, korku, heyecan, hüzün, öfke, nötr vb.)\", " +
				"\"category\": \"Rüya kategorisi (kabus, güzel rüya, tekrarlayan, lücid, kehanet, normal vb.)\", " +
				"\"symbols\": [\"sembol1\", \"sembol2\", \"sembol3\"], " +
				"\"scores\": {\"mutluluk\": 0.0-1.0, \"korku\": 0.0-1.0, \"heyecan\": 0.0-1.0, \"hüzün\": 0.0-1.0, \"öfke\": 0.0-1.0, \"sakinlik\": 0.0-1.0}} " +
				"Rüya: %s " +
				"ÖNEMLİ: Sadece JSON döndür, başka hiçbir açıklama ekleme. JSON'u markdown code block içine alma.",
				text);

		String requestBody = String.format(
				"{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}]}",
				prompt.replace("\"", "\\\"").replace("\n", " ").replace("\r", " "));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
				Map<?, ?> response = resp.getBody();
				if (response == null || !response.containsKey("candidates")) {
					log.warn("Gemini empty or no 'candidates'. Raw response: {}", response);
					return createDefaultResult("Yanıt alınamadı.");
				}
				var candidates = (List<Map<String, Object>>) response.get("candidates");
				if (candidates.isEmpty())
					return createDefaultResult("Analiz bulunamadı.");
				var content = (Map<String, Object>) candidates.get(0).get("content");
				if (content == null)
					return createDefaultResult("Yanıt alınamadı.");
				var parts = (List<Map<String, Object>>) content.get("parts");
				if (parts == null || parts.isEmpty())
					return createDefaultResult("Yanıt alınamadı.");
				Object firstPartText = parts.get(0).get("text");
				String raw = firstPartText == null ? "Yanıt alınamadı." : firstPartText.toString();
				
				// JSON'u parse et
				return parseGeminiResponse(raw);
			} catch (HttpClientErrorException httpEx) {
				String body = httpEx.getResponseBodyAsString();
				if (httpEx.getStatusCode().value() == 429 && attempt == 1) {
					continue;
				}
				log.error("Gemini HTTP {} - {}", httpEx.getStatusCode().value(), httpEx.getStatusText());
				log.error("Gemini response: {}", body);
				return createDefaultResult("Gemini API hatası: " + (body != null && !body.isBlank() ? body : "Bilinmeyen hata"));
			} catch (RestClientResponseException rce) {
				String body = rce.getResponseBodyAsString();
				log.error("Gemini error: status={} body={}", rce.getRawStatusCode(), body);
				return createDefaultResult("Gemini API hatası: " + (body != null && !body.isBlank() ? body : "Bilinmeyen hata"));
			} catch (Exception e) {
				log.error("Error parsing Gemini response: ", e);
				if (attempt == 2) {
					return createDefaultResult("Analiz parse edilemedi.");
				}
			}
		}
		return createDefaultResult("Yanıt alınamadı.");
	}

	private AnalysisResultDTO parseGeminiResponse(String rawResponse) {
		try {
			// JSON'u temizle (markdown code block varsa kaldır)
			String cleaned = rawResponse.trim();
			if (cleaned.startsWith("```json")) {
				cleaned = cleaned.substring(7);
			}
			if (cleaned.startsWith("```")) {
				cleaned = cleaned.substring(3);
			}
			if (cleaned.endsWith("```")) {
				cleaned = cleaned.substring(0, cleaned.length() - 3);
			}
			cleaned = cleaned.trim();
			
			// JSON parse et
			Map<String, Object> jsonMap = objectMapper.readValue(cleaned, Map.class);
			
			// AnalysisResultDTO oluştur
			AnalysisResultDTO result = AnalysisResultDTO.builder()
					.summary(extractString(jsonMap, "summary", "Analiz yapılamadı."))
					.dominantEmotion(extractString(jsonMap, "dominantEmotion", "nötr"))
					.category(extractString(jsonMap, "category", "normal"))
					.symbols(extractStringList(jsonMap, "symbols"))
					.scores(extractScores(jsonMap, "scores"))
					.build();
			
			return result;
		} catch (Exception e) {
			log.error("Error parsing JSON response: {}", rawResponse, e);
			// Fallback: Eğer JSON parse edilemezse, raw response'u summary olarak kullan
			return AnalysisResultDTO.builder()
					.summary(cleanPlainText(rawResponse))
					.dominantEmotion("nötr")
					.category("normal")
					.symbols(List.of())
					.scores(Map.of())
					.build();
		}
	}

	private AnalysisResultDTO createDefaultResult(String summary) {
		return AnalysisResultDTO.builder()
				.summary(summary)
				.dominantEmotion("nötr")
				.category("bilinmeyen")
				.symbols(List.of())
				.scores(Map.of())
				.build();
	}

	@SuppressWarnings("unchecked")
	private String extractString(Map<String, Object> map, String key, String defaultValue) {
		Object value = map.get(key);
		return value != null ? value.toString() : defaultValue;
	}

	@SuppressWarnings("unchecked")
	private List<String> extractStringList(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value instanceof List) {
			List<Object> list = (List<Object>) value;
			return list.stream()
					.map(Object::toString)
					.toList();
		}
		return List.of();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Double> extractScores(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value instanceof Map) {
			Map<String, Object> scoresMap = (Map<String, Object>) value;
			Map<String, Double> result = new HashMap<>();
			for (Map.Entry<String, Object> entry : scoresMap.entrySet()) {
				try {
					if (entry.getValue() instanceof Number) {
						result.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
					} else if (entry.getValue() != null) {
						result.put(entry.getKey(), Double.parseDouble(entry.getValue().toString()));
					}
				} catch (Exception e) {
					log.warn("Could not parse score for key {}: {}", entry.getKey(), entry.getValue());
				}
			}
			return result;
		}
		return Map.of();
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
                .category(result.getCategory())
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
        try {
            return objectMapper.writeValueAsString(scores);
        } catch (Exception e) {
            log.warn("Error converting scores to JSON: ", e);
            // Fallback to manual JSON building
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Double> entry : scores.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(entry.getKey().replace("\"", "\\\"")).append('"')
                  .append(':')
                  .append(entry.getValue() == null ? "null" : entry.getValue());
            }
            sb.append('}');
            return sb.toString();
        }
    }

    private Map<String, Double> parseScoresFromJson(String scoresJson) {
        if (scoresJson == null || scoresJson.isBlank()) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
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
                    log.warn("Could not parse score for key {}: {}", entry.getKey(), entry.getValue());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Error parsing scores JSON: {}", scoresJson, e);
            return Map.of();
        }
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
						.category(a.getCategory())
						.symbols(a.getSymbolsText() == null || a.getSymbolsText().isBlank() 
								? List.of() 
								: java.util.Arrays.asList(a.getSymbolsText().split(",")))
						.scores(parseScoresFromJson(a.getScoresJson()))
						.build())
				.toList();
	}
}
