package com.smu.tariff.news;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NewsDataClientTest {

    @Test
    void testExecuteRequest_apiErrorStatus() throws Exception {
        // Simulate API error in response JSON
        String errorJson = "{\"status\":\"error\",\"message\":\"bad request\",\"code\":\"400\"}";
        ResponseEntity<String> resp = new ResponseEntity<>(errorJson, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(resp);
        Throwable thrown = catchThrowable(() -> client.getLatestNews(NewsDataRequest.builder().query("tariff").build()));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to parse NewsData.io response");
        assertThat(thrown.getCause()).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("API error [400]: bad request");
    }


    @Test
    void testExecuteRequest_nullArticles() throws Exception {
        // Simulate valid response but no articles array
        String json = "{\"status\":\"success\",\"totalResults\":0}";
        ResponseEntity<String> resp = new ResponseEntity<>(json, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenReturn(resp);
        NewsDataResponse result = client.getLatestNews(NewsDataRequest.builder().query("tariff").build());
        assertThat(result).isNotNull();
        assertThat(result.getArticles()).isEmpty();
    }

    @Test
    void testParseStringArray_emptyOrNull() throws Exception {
        // Use reflection to call private method
        var method = NewsDataClient.class.getDeclaredMethod("parseStringArray", com.fasterxml.jackson.databind.JsonNode.class);
        method.setAccessible(true);
        ObjectMapper om = new ObjectMapper();
        NewsDataClient c = new NewsDataClient("key", new MockRestTemplateBuilder(restTemplate), om);
        // Null node
        com.fasterxml.jackson.databind.JsonNode nullNode = om.readTree("null");
        @SuppressWarnings("unchecked")
        List<String> result1 = (List<String>) method.invoke(c, nullNode);
        assertThat(result1).isNull();
        // Empty array
        com.fasterxml.jackson.databind.JsonNode arrNode = om.readTree("[]");
        @SuppressWarnings("unchecked")
        List<String> result2 = (List<String>) method.invoke(c, arrNode);
        assertThat(result2).isNull();
    }
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private NewsDataClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        RestTemplateBuilder builder = new MockRestTemplateBuilder(restTemplate);
        client = new NewsDataClient("test-key", builder, objectMapper);
    }

    @Test
    void testGetLatestNews_apiKeyMissing() {
        NewsDataClient badClient = new NewsDataClient("", new MockRestTemplateBuilder(restTemplate), objectMapper);
        NewsDataRequest req = NewsDataRequest.builder().query("tariff").build();
        assertThatThrownBy(() -> badClient.getLatestNews(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void testGetArchiveNews_apiKeyMissing() {
        NewsDataClient badClient = new NewsDataClient(null, new MockRestTemplateBuilder(restTemplate), objectMapper);
        NewsDataRequest req = NewsDataRequest.builder().query("tariff").build();
        assertThatThrownBy(() -> badClient.getArchiveNews(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void testGetSources_apiKeyMissing() {
        NewsDataClient badClient = new NewsDataClient(" ", new MockRestTemplateBuilder(restTemplate), objectMapper);
        NewsDataRequest req = NewsDataRequest.builder().country(List.of("us")).build();
        assertThatThrownBy(() -> badClient.getSources(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API key");
    }

    @Test
    void testGetSources_restClientException() {
        NewsDataRequest req = NewsDataRequest.builder().country(List.of("us")).build();
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new RestClientException("fail"));
        assertThatThrownBy(() -> client.getSources(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to fetch news sources");
    }

    @Test
    void testGetSources_invalidJson() {
        NewsDataRequest req = NewsDataRequest.builder().country(List.of("us")).build();
        ResponseEntity<String> resp = new ResponseEntity<>("not-json", HttpStatus.OK);
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(resp);
        assertThatThrownBy(() -> client.getSources(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse NewsData.io response");
    }

    @Test
    void testExecuteRequest_restClientException() {
    // Simulate RestClientException in executeRequest (used by getLatestNews)
    NewsDataRequest req = NewsDataRequest.builder().query("tariff").build();
    when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
        .thenThrow(new RestClientException("rest client fail"));
    assertThatThrownBy(() -> client.getLatestNews(req))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to fetch news from NewsData.io");
    }

    @Test
    void testExecuteRequest_genericException() {
    // Simulate generic Exception in executeRequest (used by getLatestNews)
    NewsDataRequest req = NewsDataRequest.builder().query("tariff").build();
    when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
        .thenThrow(new RuntimeException("unexpected error"));
    assertThatThrownBy(() -> client.getLatestNews(req))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to parse NewsData.io response");
    }
}
