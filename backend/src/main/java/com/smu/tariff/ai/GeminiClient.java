package com.smu.tariff.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GeminiClient {

    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String GENERATE_CONTENT_URL = "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Value("${gemini.api.key:}") String apiKey,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = objectMapper;
    }

    public String generateSummary(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        try {
            String url = String.format(GENERATE_CONTENT_URL, MODEL_NAME, apiKey);
            String requestBody = objectMapper.writeValueAsString(
                    Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Gemini API request failed with status " + response.getStatusCode());
            }

            JsonNode textNode = objectMapper.readTree(response.getBody())
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                throw new IllegalStateException("Gemini API response did not contain summary text");
            }

            return textNode.asText();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate Gemini summary", ex);
        }
    }
}
