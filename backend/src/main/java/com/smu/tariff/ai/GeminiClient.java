package com.smu.tariff.ai;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;


public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();

    public GeminiClient(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
    }    

    public String generateSummary(String prompt) throws Exception {
        String body = """
        {
          "contents": [
            {
              "parts": [
                {"text": "%s"}
              ]
            }
          ]
        }
        """.formatted(prompt.replace("\"", "\\\"")); // escape quotes

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body(); // TODO: parse JSON to extract text
    }
}
