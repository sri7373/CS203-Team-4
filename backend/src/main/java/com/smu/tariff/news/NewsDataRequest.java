package com.smu.tariff.news;

import java.util.List;

/**
 * Request parameters for NewsData.io API
 * Documentation: https://newsdata.io/documentation
 */
public class NewsDataRequest {

    // Search parameters
    private String query;              // q - keywords in title, content, URL, meta
    private String queryInTitle;       // qInTitle - keywords in title only
    private String queryInMeta;        // qInMeta - keywords in title, URL, meta only
    
    // Filter parameters
    private List<String> country;      // country - up to 5 (free/basic), 10 (pro/corp)
    private List<String> category;     // category - up to 5 (free/basic), 10 (pro/corp)
    private List<String> language;     // language - up to 5 (free/basic), 10 (pro/corp)
    private List<String> domain;       // domain - news source names
    private List<String> domainUrl;    // domainurl - news source URLs
    
    // Date/Time parameters
    private String fromDate;           // from_date - YYYY-MM-DD or YYYY-MM-DD HH:MM:SS
    private String toDate;             // to_date - YYYY-MM-DD or YYYY-MM-DD HH:MM:SS
    private String timeframe;          // timeframe - 1-48 hours or 1m-2880m minutes
    
    // AI parameters (paid plans only)
    private String sentiment;          // sentiment - positive, negative, neutral
    private List<String> aiTag;        // tag - AI-classified tags
    
    // Content filters
    private Boolean fullContent;       // full_content - 1 or 0
    private Boolean image;             // image - 1 or 0
    private Boolean video;             // video - 1 or 0
    private Boolean removeDuplicate;   // removeduplicate - 1 or 0
    
    // Sorting and priority
    private String priorityDomain;     // prioritydomain - top, medium, low
    
    // Pagination
    private Integer size;              // size - 1-10 (free), 1-50 (paid)
    private String page;               // page - next page token

    // Constructors
    public NewsDataRequest() {
    }

    public static Builder builder() {
        return new Builder();
    }

    // Builder pattern for easy request construction
    public static class Builder {
        private final NewsDataRequest request = new NewsDataRequest();

        public Builder query(String query) {
            request.query = query;
            return this;
        }

        public Builder queryInTitle(String queryInTitle) {
            request.queryInTitle = queryInTitle;
            return this;
        }

        public Builder queryInMeta(String queryInMeta) {
            request.queryInMeta = queryInMeta;
            return this;
        }

        public Builder country(List<String> country) {
            request.country = country;
            return this;
        }

        public Builder category(List<String> category) {
            request.category = category;
            return this;
        }

        public Builder language(List<String> language) {
            request.language = language;
            return this;
        }

        public Builder domain(List<String> domain) {
            request.domain = domain;
            return this;
        }

        public Builder domainUrl(List<String> domainUrl) {
            request.domainUrl = domainUrl;
            return this;
        }

        public Builder fromDate(String fromDate) {
            request.fromDate = fromDate;
            return this;
        }

        public Builder toDate(String toDate) {
            request.toDate = toDate;
            return this;
        }

        public Builder timeframe(String timeframe) {
            request.timeframe = timeframe;
            return this;
        }

        public Builder sentiment(String sentiment) {
            request.sentiment = sentiment;
            return this;
        }

        public Builder aiTag(List<String> aiTag) {
            request.aiTag = aiTag;
            return this;
        }

        public Builder fullContent(Boolean fullContent) {
            request.fullContent = fullContent;
            return this;
        }

        public Builder image(Boolean image) {
            request.image = image;
            return this;
        }

        public Builder video(Boolean video) {
            request.video = video;
            return this;
        }

        public Builder removeDuplicate(Boolean removeDuplicate) {
            request.removeDuplicate = removeDuplicate;
            return this;
        }

        public Builder priorityDomain(String priorityDomain) {
            request.priorityDomain = priorityDomain;
            return this;
        }

        public Builder size(Integer size) {
            request.size = size;
            return this;
        }

        public Builder page(String page) {
            request.page = page;
            return this;
        }

        public NewsDataRequest build() {
            return request;
        }
    }

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQueryInTitle() {
        return queryInTitle;
    }

    public void setQueryInTitle(String queryInTitle) {
        this.queryInTitle = queryInTitle;
    }

    public String getQueryInMeta() {
        return queryInMeta;
    }

    public void setQueryInMeta(String queryInMeta) {
        this.queryInMeta = queryInMeta;
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

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }

    public List<String> getDomain() {
        return domain;
    }

    public void setDomain(List<String> domain) {
        this.domain = domain;
    }

    public List<String> getDomainUrl() {
        return domainUrl;
    }

    public void setDomainUrl(List<String> domainUrl) {
        this.domainUrl = domainUrl;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public List<String> getAiTag() {
        return aiTag;
    }

    public void setAiTag(List<String> aiTag) {
        this.aiTag = aiTag;
    }

    public Boolean getFullContent() {
        return fullContent;
    }

    public void setFullContent(Boolean fullContent) {
        this.fullContent = fullContent;
    }

    public Boolean getImage() {
        return image;
    }

    public void setImage(Boolean image) {
        this.image = image;
    }

    public Boolean getVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        this.video = video;
    }

    public Boolean getRemoveDuplicate() {
        return removeDuplicate;
    }

    public void setRemoveDuplicate(Boolean removeDuplicate) {
        this.removeDuplicate = removeDuplicate;
    }

    public String getPriorityDomain() {
        return priorityDomain;
    }

    public void setPriorityDomain(String priorityDomain) {
        this.priorityDomain = priorityDomain;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }
}
