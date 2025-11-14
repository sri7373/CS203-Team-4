package com.smu.tariff.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeminiClientTest {
    RestTemplate restTemplate;
    ObjectMapper objectMapper;
    GeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.setConnectTimeout(any())).thenReturn(builder);
        when(builder.setReadTimeout(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        geminiClient = new GeminiClient("test-api-key", builder, objectMapper);
    }

    @Test
    void testGenerateSummary_success() throws Exception {
        String prompt = "Summarize this.";
        String apiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"summary result\"}]}}]}";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(apiResponse);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(responseEntity);

        String result = geminiClient.generateSummary(prompt);
        assertThat(result).isEqualTo("summary result");
    }

    @Test
    void testGenerateSummary_noApiKey() {
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.setConnectTimeout(any())).thenReturn(builder);
        when(builder.setReadTimeout(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restTemplate);
        GeminiClient client = new GeminiClient("", builder, objectMapper);
        assertThatThrownBy(() -> client.generateSummary("prompt")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gemini API key is not configured");
    }

    @Test
    void testGenerateSummary_apiError() {
    String prompt = "Summarize this.";
    ResponseEntity<String> responseEntity = ResponseEntity.status(500).body(null);
    when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(responseEntity);
    assertThatThrownBy(() -> geminiClient.generateSummary(prompt)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to generate Gemini summary");
    }
}
