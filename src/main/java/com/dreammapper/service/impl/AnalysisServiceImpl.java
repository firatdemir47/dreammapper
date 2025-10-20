package com.dreammapper.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.dreammapper.service.AnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {
	@Value("${gemini.api.key}")
	private String geminiApiKey;

	private static final Logger log = LoggerFactory.getLogger(AnalysisServiceImpl.class);
	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public AnalysisResultDTO analyzeDream(AnalysisRequestDTO request) {
		String text = Optional.ofNullable(request.getText()).orElse("");

		try {
			String aiResponse = callGemini(text);
			return AnalysisResultDTO.builder().summary(aiResponse).dominantEmotion("AI Analizi").symbols(List.of())
					.scores(Map.of()).build();
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
}
