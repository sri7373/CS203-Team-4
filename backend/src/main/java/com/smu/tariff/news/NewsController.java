package com.smu.tariff.news;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for news-related endpoints
 * Provides access to tariff and trade news from NewsData.io
 */
@RestController
@RequestMapping("/api/news")
@Tag(name = "News", description = "Tariff and trade news from NewsData.io")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Get latest tariff and trade news with strict filtering
     * 
     * @param country Optional ISO 2-letter country code filter (e.g., "us", "cn", "gb")
     * @param productCategory Optional product category filter (e.g., "steel", "electronics", "agriculture", "automotive")
     * @param limit Number of articles to return (default: 10, max: 50)
     * @return Response with news articles
     */
    @GetMapping("/tariff")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get latest tariff news",
        description = "Fetches the latest news articles about tariffs, import duties, and trade policies. " +
                      "Uses strict filtering to ensure relevance. Supports filtering by country and product category. " +
                      "Articles are from the past 48 hours and sourced from top business and politics news outlets."
    )
    public ResponseEntity<NewsDataResponse> getLatestTariffNews(
            @Parameter(description = "ISO 2-letter country code (e.g., 'us', 'cn', 'gb')")
            @RequestParam(required = false) String country,
            @Parameter(description = "Product category (e.g., 'steel', 'electronics', 'agriculture', 'automotive', 'textiles')")
            @RequestParam(required = false) String productCategory,
            @Parameter(description = "Number of articles (1-50, default: 10)")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        NewsDataResponse response = newsService.getLatestTariffNews(country, productCategory, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Search for news articles with custom query
     * 
     * @param query Search keywords (supports AND, OR, NOT operators)
     * @param country Optional country filter
     * @param limit Number of articles
     * @return Response with matching articles
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Search news articles",
        description = "Search for news articles using custom keywords. " +
                      "Supports advanced search with AND, OR, NOT operators. " +
                      "Examples: 'tariff AND china', 'trade OR customs', 'import NOT export'"
    )
    public ResponseEntity<NewsDataResponse> searchNews(
            @Parameter(description = "Search keywords (supports AND, OR, NOT)", required = true)
            @RequestParam String query,
            @Parameter(description = "ISO 2-letter country code")
            @RequestParam(required = false) String country,
            @Parameter(description = "Number of articles (1-50, default: 10)")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        NewsDataResponse response = newsService.searchNews(query, country, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get trade news for a specific country with optional product category
     * 
     * @param countryCode ISO 2-letter country code (required)
     * @param productCategory Optional product category filter
     * @param limit Number of articles
     * @return Response with country-specific trade news
     */
    @GetMapping("/country")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get country trade news",
        description = "Get news articles about a specific country's trade policies, tariffs, and customs regulations. " +
                      "Optionally filter by product category (e.g., 'steel', 'electronics', 'agriculture')."
    )
    public ResponseEntity<NewsDataResponse> getCountryTradeNews(
            @Parameter(description = "ISO 2-letter country code (required)", required = true)
            @RequestParam String countryCode,
            @Parameter(description = "Product category (e.g., 'steel', 'electronics', 'agriculture')")
            @RequestParam(required = false) String productCategory,
            @Parameter(description = "Number of articles (1-50, default: 10)")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        NewsDataResponse response = newsService.getCountryTradeNews(countryCode, productCategory, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get historical tariff news (requires NewsData.io paid plan with archive access)
     * 
     * @param fromDate Start date (YYYY-MM-DD format, required)
     * @param toDate End date (YYYY-MM-DD format, required)
     * @param country Optional country filter
     * @param limit Number of articles
     * @return Response with historical articles
     */
    @GetMapping("/archive")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get historical tariff news",
        description = "Get historical news articles about tariffs and trade. " +
                      "Requires NewsData.io paid plan with archive access. " +
                      "Archive limits: Basic (6 months), Professional (2 years), Corporate (5 years)."
    )
    public ResponseEntity<NewsDataResponse> getHistoricalNews(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam String fromDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam String toDate,
            @Parameter(description = "ISO 2-letter country code")
            @RequestParam(required = false) String country,
            @Parameter(description = "Number of articles (1-50, default: 10)")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        NewsDataResponse response = newsService.getHistoricalTariffNews(fromDate, toDate, country, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get next page of results using pagination token
     * 
     * @param nextPage Pagination token from previous response (required)
     * @return Response with next page of articles
     */
    @GetMapping("/next")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get next page of results",
        description = "Fetch the next page of news articles using the pagination token (nextPage) from a previous response."
    )
    public ResponseEntity<NewsDataResponse> getNextPage(
            @Parameter(description = "Pagination token from previous response", required = true)
            @RequestParam String nextPage) {
        
        NewsDataResponse response = newsService.getNextPage(nextPage);
        return ResponseEntity.ok(response);
    }

    /**
     * Get available news sources for tariff content
     * 
     * @param country Optional country filter
     * @return List of news sources
     */
    @GetMapping("/sources")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get news sources",
        description = "Get a list of available news sources that cover tariff and trade topics. " +
                      "Returns up to 100 randomly selected sources matching the criteria."
    )
    public ResponseEntity<List<NewsSource>> getNewsSources(
            @Parameter(description = "ISO 2-letter country code")
            @RequestParam(required = false) String country) {
        
        List<NewsSource> sources = newsService.getTariffNewsSources(country);
        return ResponseEntity.ok(sources);
    }
}
