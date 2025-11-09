package com.smu.tariff.news;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Service layer for news operations
 * Provides business logic for fetching and processing tariff-related news
 */
@Service
public class NewsService {

    private final NewsDataClient newsDataClient;

    public NewsService(NewsDataClient newsDataClient) {
        this.newsDataClient = newsDataClient;
    }

    /**
     * Get latest tariff-related news with strict filtering
     * Focuses on tariff rates, duties, trade policy changes, and customs regulations
     * 
     * @param country Optional country filter (e.g., "us", "cn", "gb")
     * @param productCategory Optional product category filter (e.g., "steel", "electronics", "agriculture")
     * @param limit Number of articles to return (1-50)
     * @return NewsDataResponse with articles
     */
    public NewsDataResponse getLatestTariffNews(String country, String productCategory, Integer limit) {
        // Build strict tariff-focused query
        StringBuilder queryBuilder = new StringBuilder("(tariff OR \"import duty\" OR \"customs duty\")");
        queryBuilder.append(" AND trade");

        if (productCategory != null && !productCategory.isBlank()) {
            String category = productCategory.toLowerCase().trim().replace("\"", "");
            queryBuilder.append(" AND \"").append(category).append("\"");
        }

        String query = queryBuilder.toString();
        if (query.length() > 100) {
            query = "(tariff OR \"import duty\" OR \"customs duty\")";
        }
        
        NewsDataRequest.Builder builder = NewsDataRequest.builder()
                .query(query)
                .category(Arrays.asList("business", "politics", "world"))
                .language(Arrays.asList("en"))
                .priorityDomain("top")
                .removeDuplicate(true)
                .image(true)  // Prefer articles with images for better UI
                .size(limit != null ? Math.min(limit, 50) : 10);

        if (country != null && !country.isBlank()) {
            builder.country(Arrays.asList(country.toLowerCase()));
        }

        return newsDataClient.getLatestNews(builder.build());
    }
    
    /**
     * Overloaded method for backward compatibility
     */
    public NewsDataResponse getLatestTariffNews(String country, Integer limit) {
        return getLatestTariffNews(country, null, limit);
    }

    /**
     * Get news for a specific search query
     * 
     * @param query Search keywords
     * @param country Optional country filter
     * @param limit Number of articles
     * @return NewsDataResponse with articles
     */
    public NewsDataResponse searchNews(String query, String country, Integer limit) {
        NewsDataRequest.Builder builder = NewsDataRequest.builder()
                .query(query)
                .language(Arrays.asList("en"))
                .removeDuplicate(true)
                .size(limit != null ? Math.min(limit, 50) : 10);

        if (country != null && !country.isBlank()) {
            builder.country(Arrays.asList(country));
        }

        return newsDataClient.getLatestNews(builder.build());
    }

    /**
     * Get news about a specific country's trade policies with product category filter
     * 
     * @param countryCode ISO 2-letter country code
     * @param productCategory Optional product category (e.g., "steel", "electronics")
     * @param limit Number of articles
     * @return NewsDataResponse with articles
     */
    public NewsDataResponse getCountryTradeNews(String countryCode, String productCategory, Integer limit) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("(\"trade policy\" OR tariff OR \"customs duty\" OR \"import tax\" OR \"trade war\")");
        
        if (productCategory != null && !productCategory.isBlank()) {
            String category = productCategory.toLowerCase().trim();
            queryBuilder.append(" AND (")
                       .append(category)
                       .append(" OR \"").append(category).append(" sector\"")
                       .append(" OR \"").append(category).append(" industry\"")
                       .append(")");
        }
        
        NewsDataRequest request = NewsDataRequest.builder()
                .query(queryBuilder.toString())
                .country(Arrays.asList(countryCode.toLowerCase()))
                .category(Arrays.asList("business", "politics", "world"))
                .language(Arrays.asList("en"))
                .priorityDomain("top")
                .removeDuplicate(true)
                .image(true)
                .size(limit != null ? Math.min(limit, 50) : 10)
                .build();

        return newsDataClient.getLatestNews(request);
    }
    
    /**
     * Overloaded method for backward compatibility
     */
    public NewsDataResponse getCountryTradeNews(String countryCode, Integer limit) {
        return getCountryTradeNews(countryCode, null, limit);
    }

    /**
     * Get historical tariff news (requires paid plan with archive access)
     * 
     * @param fromDate Start date (YYYY-MM-DD)
     * @param toDate End date (YYYY-MM-DD)
     * @param country Optional country filter
     * @param limit Number of articles
     * @return NewsDataResponse with articles
     */
    public NewsDataResponse getHistoricalTariffNews(String fromDate, String toDate, String country, Integer limit) {
        NewsDataRequest.Builder builder = NewsDataRequest.builder()
                .query("tariff OR trade OR customs")
                .fromDate(fromDate)
                .toDate(toDate)
                .category(Arrays.asList("business", "politics"))
                .language(Arrays.asList("en"))
                .removeDuplicate(true)
                .size(limit != null ? Math.min(limit, 50) : 10);

        if (country != null && !country.isBlank()) {
            builder.country(Arrays.asList(country));
        }

        return newsDataClient.getArchiveNews(builder.build());
    }

    /**
     * Get next page of results using pagination token
     * 
     * @param nextPageToken Token from previous response
     * @return NewsDataResponse with next page of articles
     */
    public NewsDataResponse getNextPage(String nextPageToken) {
        if (nextPageToken == null || nextPageToken.isBlank()) {
            throw new IllegalArgumentException("Next page token is required");
        }

        NewsDataRequest request = NewsDataRequest.builder()
                .page(nextPageToken)
                .build();

        return newsDataClient.getLatestNews(request);
    }

    /**
     * Get available news sources for tariff-related content
     * 
     * @param country Optional country filter
     * @return List of news sources
     */
    public List<NewsSource> getTariffNewsSources(String country) {
        NewsDataRequest.Builder builder = NewsDataRequest.builder()
                .category(Arrays.asList("business", "politics"))
                .language(Arrays.asList("en"));

        if (country != null && !country.isBlank()) {
            builder.country(Arrays.asList(country));
        }

        return newsDataClient.getSources(builder.build());
    }
}
