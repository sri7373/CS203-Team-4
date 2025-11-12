package com.smu.tariff.news;

import java.util.List;

/**
 * Response wrapper for NewsData.io API
 */
public class NewsDataResponse {
    
    private String status;
    private Integer totalResults;
    private List<NewsArticle> articles;
    private String nextPage;  // Pagination token

    public NewsDataResponse() {
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public List<NewsArticle> getArticles() {
        return articles;
    }

    public void setArticles(List<NewsArticle> articles) {
        this.articles = articles;
    }

    public String getNextPage() {
        return nextPage;
    }

    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
    }
}
