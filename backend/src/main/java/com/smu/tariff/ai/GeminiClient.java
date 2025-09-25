package com.smu.tariff.ai;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiClient(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }    

    public String generateSummary(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        String requestJson = String.format(
            "{ \"contents\": [{ \"parts\": [{ \"text\": \"%s\" }] }] }",
            prompt.replace("\"", "\\\"") // escape quotes
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            String.class
        );

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            // Drill down: candidates[0].content.parts[0].text
            return root.path("candidates")
                       .get(0)
                       .path("content")
                       .path("parts")
                       .get(0)
                       .path("text")
                       .asText();
        } catch (Exception e) {
            e.printStackTrace();
            return "AI summary generation failed";
        }
    }
}
