package com.smu.tariff.news;

import java.util.List;

/**
 * News article model based on NewsData.io response structure
 * Documentation: https://newsdata.io/documentation#response-object
 */
public class NewsArticle {

    // Basic article information
    private String articleId;
    private String title;
    private String link;
    private String description;
    private String content;
    private String pubDate;
    private String pubDateTZ;
    
    // Media
    private String imageUrl;
    private String videoUrl;
    
    // Source information
    private String sourceId;
    private String sourceUrl;
    private String sourceIcon;
    private Integer sourcePriority;
    
    // Classification
    private List<String> keywords;
    private List<String> creator;
    private List<String> country;
    private List<String> category;
    private String language;
    
    // AI-enhanced fields (paid plans only)
    private String aiTag;
    private String sentiment;
    private String sentimentStats;
    private String aiRegion;
    private String aiOrg;
    private String aiSummary;
    private String aiContent;
    
    // Duplicate detection
    private Boolean duplicate;

    public NewsArticle() {
    }

    // Getters and Setters
    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getPubDateTZ() {
        return pubDateTZ;
    }

    public void setPubDateTZ(String pubDateTZ) {
        this.pubDateTZ = pubDateTZ;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceIcon() {
        return sourceIcon;
    }

    public void setSourceIcon(String sourceIcon) {
        this.sourceIcon = sourceIcon;
    }

    public Integer getSourcePriority() {
        return sourcePriority;
    }

    public void setSourcePriority(Integer sourcePriority) {
        this.sourcePriority = sourcePriority;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getCreator() {
        return creator;
    }

    public void setCreator(List<String> creator) {
        this.creator = creator;
    }

    public List<String> getCountry() {
        return country;
    }

    public void setCountry(List<String> country) {
        this.country = country;
    }

    public List<String> getCategory() {
        return category;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAiTag() {
        return aiTag;
    }

    public void setAiTag(String aiTag) {
        this.aiTag = aiTag;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getSentimentStats() {
        return sentimentStats;
    }

    public void setSentimentStats(String sentimentStats) {
        this.sentimentStats = sentimentStats;
    }

    public String getAiRegion() {
        return aiRegion;
    }

    public void setAiRegion(String aiRegion) {
        this.aiRegion = aiRegion;
    }

    public String getAiOrg() {
        return aiOrg;
    }

    public void setAiOrg(String aiOrg) {
        this.aiOrg = aiOrg;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiContent() {
        return aiContent;
    }

    public void setAiContent(String aiContent) {
        this.aiContent = aiContent;
    }

    public Boolean getDuplicate() {
        return duplicate;
    }

    public void setDuplicate(Boolean duplicate) {
        this.duplicate = duplicate;
    }
}
