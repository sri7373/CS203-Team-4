package com.smu.tariff.news;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Client for NewsData.io API
 * Documentation: https://newsdata.io/documentation
 * 
 * Authentication: API key can be passed as query parameter (apikey) or header (X-ACCESS-KEY)
 * This implementation uses query parameter as per NewsData.io documentation
 */
@Component
public class NewsDataClient {

    private static final String BASE_URL = "https://newsdata.io/api/1";
    private static final String LATEST_ENDPOINT = "/latest";
    private static final String ARCHIVE_ENDPOINT = "/archive";
    private static final String SOURCES_ENDPOINT = "/sources";

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NewsDataClient(
            @Value("${newsdata.api.key:}") String apiKey,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch latest news articles (past 48 hours)
     * 
     * @param request - NewsData request parameters
     * @return NewsDataResponse containing articles and pagination info
     * @throws IllegalStateException if API key is not configured or request fails
     */
    public NewsDataResponse getLatestNews(NewsDataRequest request) {
        validateApiKey();
        
        String url = buildUrl(LATEST_ENDPOINT, request);
        return executeRequest(url);
    }

    /**
     * Fetch historical news articles (requires paid plan)
     * 
     * @param request - NewsData request parameters (must include from_date/to_date or other filters)
     * @return NewsDataResponse containing articles and pagination info
     * @throws IllegalStateException if API key is not configured or request fails
     */
    public NewsDataResponse getArchiveNews(NewsDataRequest request) {
        validateApiKey();
        
        String url = buildUrl(ARCHIVE_ENDPOINT, request);
        return executeRequest(url);
    }

    /**
     * Fetch news sources available on NewsData.io
     * 
     * @param request - NewsData request parameters (country, category, language, etc.)
     * @return List of news sources
     * @throws IllegalStateException if API key is not configured or request fails
     */
    public List<NewsSource> getSources(NewsDataRequest request) {
        validateApiKey();
        
        String url = buildUrl(SOURCES_ENDPOINT, request);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TariffSheriff/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("NewsData.io API request failed with status " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Check for error response
            if (root.has("status") && "error".equals(root.get("status").asText())) {
                String errorMessage = root.has("message") ? root.get("message").asText() : "Unknown error";
                throw new IllegalStateException("NewsData.io API error: " + errorMessage);
            }

            // Parse sources
            List<NewsSource> sources = new ArrayList<>();
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode sourceNode : results) {
                    sources.add(parseNewsSource(sourceNode));
                }
            }

            return sources;

        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to fetch news sources from NewsData.io", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse NewsData.io response", ex);
        }
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("NewsData.io API key is not configured. Please add 'newsdata.api.key' to application.yml");
        }
    }

    private String buildUrl(String endpoint, NewsDataRequest request) {
        StringBuilder url = new StringBuilder(BASE_URL + endpoint);
        url.append("?apikey=").append(apiKey);

        if (request.getQuery() != null && !request.getQuery().isBlank()) {
            url.append("&q=").append(encode(request.getQuery()));
        }
        if (request.getQueryInTitle() != null && !request.getQueryInTitle().isBlank()) {
            url.append("&qInTitle=").append(encode(request.getQueryInTitle()));
        }
        if (request.getCountry() != null && !request.getCountry().isEmpty()) {
            url.append("&country=").append(String.join(",", request.getCountry()));
        }
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            url.append("&category=").append(String.join(",", request.getCategory()));
        }
        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            url.append("&language=").append(String.join(",", request.getLanguage()));
        }
        if (request.getDomain() != null && !request.getDomain().isEmpty()) {
            url.append("&domain=").append(String.join(",", request.getDomain()));
        }
        if (request.getDomainUrl() != null && !request.getDomainUrl().isEmpty()) {
            url.append("&domainurl=").append(String.join(",", request.getDomainUrl()));
        }
        if (request.getFromDate() != null && !request.getFromDate().isBlank()) {
            url.append("&from_date=").append(request.getFromDate());
        }
        if (request.getToDate() != null && !request.getToDate().isBlank()) {
            url.append("&to_date=").append(request.getToDate());
        }
        if (request.getTimeframe() != null && !request.getTimeframe().isBlank()) {
            url.append("&timeframe=").append(request.getTimeframe());
        }
        if (request.getSentiment() != null && !request.getSentiment().isBlank()) {
            url.append("&sentiment=").append(request.getSentiment());
        }
        if (request.getPriorityDomain() != null && !request.getPriorityDomain().isBlank()) {
            url.append("&prioritydomain=").append(request.getPriorityDomain());
        }
        if (request.getSize() != null && request.getSize() > 0) {
            url.append("&size=").append(request.getSize());
        }
        if (request.getPage() != null && !request.getPage().isBlank()) {
            url.append("&page=").append(request.getPage());
        }
        if (request.getFullContent() != null) {
            url.append("&full_content=").append(request.getFullContent() ? "1" : "0");
        }
        if (request.getImage() != null) {
            url.append("&image=").append(request.getImage() ? "1" : "0");
        }
        if (request.getRemoveDuplicate() != null && request.getRemoveDuplicate()) {
            url.append("&removeduplicate=1");
        }

        return url.toString();
    }

    private NewsDataResponse executeRequest(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TariffSheriff/1.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("NewsData.io API request failed with status " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Check for error response
            if (root.has("status") && "error".equals(root.get("status").asText())) {
                String errorMessage = root.has("message") ? root.get("message").asText() : "Unknown error";
                String errorCode = root.has("code") ? root.get("code").asText() : "UNKNOWN";
                throw new IllegalStateException("NewsData.io API error [" + errorCode + "]: " + errorMessage);
            }

            // Parse response
            NewsDataResponse newsResponse = new NewsDataResponse();
            newsResponse.setStatus(root.path("status").asText());
            newsResponse.setTotalResults(root.path("totalResults").asInt(0));
            newsResponse.setNextPage(root.path("nextPage").asText(null));

            // Parse articles
            List<NewsArticle> articles = new ArrayList<>();
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode articleNode : results) {
                    articles.add(parseNewsArticle(articleNode));
                }
            }
            newsResponse.setArticles(articles);

            return newsResponse;

        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to fetch news from NewsData.io", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse NewsData.io response", ex);
        }
    }

    private NewsArticle parseNewsArticle(JsonNode node) {
        NewsArticle article = new NewsArticle();
        article.setArticleId(node.path("article_id").asText(null));
        article.setTitle(node.path("title").asText(null));
        article.setLink(node.path("link").asText(null));
        article.setDescription(node.path("description").asText(null));
        article.setContent(node.path("content").asText(null));
        article.setPubDate(node.path("pubDate").asText(null));
        article.setPubDateTZ(node.path("pubDateTZ").asText(null));
        article.setImageUrl(node.path("image_url").asText(null));
        article.setVideoUrl(node.path("video_url").asText(null));
        article.setSourceId(node.path("source_id").asText(null));
        article.setSourceUrl(node.path("source_url").asText(null));
        article.setSourceIcon(node.path("source_icon").asText(null));
        article.setSourcePriority(node.path("source_priority").isNull() ? null : node.path("source_priority").asInt());
        article.setLanguage(node.path("language").asText(null));
        
        // Parse array fields
        article.setCountry(parseStringArray(node.path("country")));
        article.setCategory(parseStringArray(node.path("category")));
        article.setCreator(parseStringArray(node.path("creator")));
        article.setKeywords(parseStringArray(node.path("keywords")));
        
        // Parse AI fields (paid plans only)
        article.setAiTag(node.path("ai_tag").asText(null));
        article.setSentiment(node.path("sentiment").asText(null));
        article.setSentimentStats(node.path("sentiment_stats").asText(null));
        article.setAiRegion(node.path("ai_region").asText(null));
        article.setAiOrg(node.path("ai_org").asText(null));
        article.setAiSummary(node.path("ai_summary").asText(null));
        article.setAiContent(node.path("ai_content").asText(null));
        
        article.setDuplicate(node.path("duplicate").asBoolean(false));
        
        return article;
    }

    private NewsSource parseNewsSource(JsonNode node) {
        NewsSource source = new NewsSource();
        source.setId(node.path("id").asText(null));
        source.setName(node.path("name").asText(null));
        source.setUrl(node.path("url").asText(null));
        source.setCategory(parseStringArray(node.path("category")));
        source.setLanguage(parseStringArray(node.path("language")));
        source.setCountry(parseStringArray(node.path("country")));
        return source;
    }

    private List<String> parseStringArray(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                result.add(item.asText());
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
