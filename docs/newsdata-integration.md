# NewsData.io Integration Guide

## Overview

This integration provides real-time and historical news about tariffs, trade policies, customs, and import/export regulations using the [NewsData.io API](https://newsdata.io).

## Features

- ✅ Latest tariff and trade news (past 48 hours)
- ✅ Country-specific trade news filtering
- ✅ Custom keyword search with advanced operators
- ✅ Historical news archive (requires paid plan)
- ✅ Pagination support for large result sets
- ✅ News source discovery
- ✅ AI-powered summaries and sentiment analysis (paid plans)

## API Key Setup

### Step 1: Get Your NewsData.io API Key

1. **Sign up for a free account**: Visit [https://newsdata.io/register](https://newsdata.io/register)
2. **Verify your email**: Check your inbox for a verification email
3. **Login to dashboard**: Go to [https://newsdata.io/login](https://newsdata.io/login)
4. **Copy your API key**: You'll see your API key on the dashboard

### Step 2: Configure Your Application

**Location**: `backend/src/main/resources/application.yml`

Find this section:

```yaml
# NewsData.io API Configuration
# Get your API key from: https://newsdata.io/register
# Place your API key below (replace YOUR_NEWSDATA_API_KEY with actual key)
newsdata:
  api:
    key: YOUR_NEWSDATA_API_KEY
```

**Replace** `YOUR_NEWSDATA_API_KEY` with your actual API key:

```yaml
newsdata:
  api:
    key: pub_123456abcdef7890xyz # Your actual key from newsdata.io dashboard
```

### Step 3: Verify Setup

After adding your API key:

1. Restart your Spring Boot application
2. Test the integration using the Swagger UI or API endpoints (see below)

## API Endpoints

All endpoints require authentication (`@PreAuthorize("isAuthenticated()")`).

### 1. Get Latest Tariff News

**Endpoint**: `GET /api/news/tariff`

**Description**: Fetches the latest news articles about tariffs, trade, customs, and import/export from the past 48 hours.

**Parameters**:

- `country` (optional): ISO 2-letter country code (e.g., `us`, `cn`, `gb`, `jp`)
- `limit` (optional): Number of articles to return (1-50, default: 10)

**Example Request**:

```bash
# Get latest tariff news from all countries
GET /api/news/tariff?limit=20

# Get latest tariff news from United States
GET /api/news/tariff?country=us&limit=15

# Get latest tariff news from China
GET /api/news/tariff?country=cn&limit=10
```

**Example Response**:

```json
{
  "status": "success",
  "totalResults": 1250,
  "articles": [
    {
      "articleId": "abc123",
      "title": "New US Tariffs on Steel Imports Take Effect",
      "link": "https://example.com/article",
      "description": "The United States has implemented new tariffs on steel imports...",
      "content": "Full article content here...",
      "pubDate": "2025-10-24 14:30:00",
      "pubDateTZ": "UTC",
      "imageUrl": "https://example.com/image.jpg",
      "sourceId": "reuters",
      "sourceUrl": "https://reuters.com",
      "sourcePriority": 1,
      "country": ["us"],
      "category": ["business", "politics"],
      "language": "en",
      "keywords": ["tariff", "steel", "trade", "import"],
      "sentiment": "neutral",
      "aiSummary": "US announces 25% tariffs on steel imports effective immediately..."
    }
  ],
  "nextPage": "xyz789abc"
}
```

### 2. Search News Articles

**Endpoint**: `GET /api/news/search`

**Description**: Search for news articles using custom keywords with advanced search operators.

**Parameters**:

- `query` (required): Search keywords (supports `AND`, `OR`, `NOT` operators)
- `country` (optional): ISO 2-letter country code
- `limit` (optional): Number of articles (1-50, default: 10)

**Search Operators**:
| Operator | Example | Description |
|----------|---------|-------------|
| Single keyword | `tariff` | Articles containing "tariff" |
| Exact phrase | `"trade war"` | Articles with exact phrase "trade war" |
| AND | `tariff AND china` | Articles containing both "tariff" and "china" |
| OR | `tariff OR customs` | Articles containing either "tariff" or "customs" |
| NOT | `trade NOT brexit` | Articles with "trade" but not "brexit" |
| Combined | `(tariff OR duty) AND china NOT steel` | Complex queries |

**Example Requests**:

```bash
# Search for China tariff news
GET /api/news/search?query=tariff%20AND%20china&limit=15

# Search for trade war articles
GET /api/news/search?query=%22trade%20war%22&limit=10

# Search for EU customs but exclude Brexit
GET /api/news/search?query=customs%20AND%20eu%20NOT%20brexit&country=gb&limit=20
```

### 3. Get Country Trade News

**Endpoint**: `GET /api/news/country`

**Description**: Get news articles about a specific country's trade policies, tariffs, and customs regulations.

**Parameters**:

- `countryCode` (required): ISO 2-letter country code
- `limit` (optional): Number of articles (1-50, default: 10)

**Example Requests**:

```bash
# Get China trade news
GET /api/news/country?countryCode=cn&limit=20

# Get Japan trade news
GET /api/news/country?countryCode=jp&limit=15
```

### 4. Get Historical Tariff News

**Endpoint**: `GET /api/news/archive`

**Description**: Get historical news articles about tariffs and trade.

**Requirements**:

- ⚠️ Requires NewsData.io **paid plan** with archive access
- Free plan: No archive access
- Basic plan: 6 months of history
- Professional plan: 2 years of history
- Corporate plan: 5 years of history

**Parameters**:

- `fromDate` (required): Start date in `YYYY-MM-DD` format
- `toDate` (required): End date in `YYYY-MM-DD` format
- `country` (optional): ISO 2-letter country code
- `limit` (optional): Number of articles (1-50, default: 10)

**Example Requests**:

```bash
# Get tariff news from January 2025
GET /api/news/archive?fromDate=2025-01-01&toDate=2025-01-31&limit=20

# Get US tariff news from Q1 2025
GET /api/news/archive?fromDate=2025-01-01&toDate=2025-03-31&country=us&limit=50
```

### 5. Get Next Page of Results

**Endpoint**: `GET /api/news/next`

**Description**: Fetch the next page of news articles using the pagination token from a previous response.

**Parameters**:

- `nextPage` (required): Pagination token from previous response's `nextPage` field

**Example Request**:

```bash
# Get next page using token from previous response
GET /api/news/next?nextPage=xyz789abc
```

**Pagination Workflow**:

```javascript
// Step 1: Get first page
const response1 = await fetch("/api/news/tariff?limit=10");
const data1 = await response1.json();
console.log("Page 1:", data1.articles);

// Step 2: Check if more pages exist
if (data1.nextPage) {
  // Step 3: Get next page
  const response2 = await fetch(`/api/news/next?nextPage=${data1.nextPage}`);
  const data2 = await response2.json();
  console.log("Page 2:", data2.articles);

  // Step 4: Continue pagination
  if (data2.nextPage) {
    // Fetch page 3, 4, etc.
  }
}
```

### 6. Get News Sources

**Endpoint**: `GET /api/news/sources`

**Description**: Get a list of available news sources that cover tariff and trade topics. Returns up to 100 randomly selected sources.

**Parameters**:

- `country` (optional): ISO 2-letter country code

**Example Requests**:

```bash
# Get all tariff news sources
GET /api/news/sources

# Get US tariff news sources
GET /api/news/sources?country=us
```

**Example Response**:

```json
[
  {
    "id": "reuters",
    "name": "Reuters",
    "url": "https://reuters.com",
    "category": ["business", "politics"],
    "language": ["en"],
    "country": ["us", "gb"]
  },
  {
    "id": "bloomberg",
    "name": "Bloomberg",
    "url": "https://bloomberg.com",
    "category": ["business"],
    "language": ["en"],
    "country": ["us"]
  }
]
```

## Code Usage Examples

### Example 1: Basic Usage in Java

```java
@Service
public class MyTariffService {

    private final NewsService newsService;

    public MyTariffService(NewsService newsService) {
        this.newsService = newsService;
    }

    public void printLatestTariffNews() {
        // Get latest tariff news from all countries
        NewsDataResponse response = newsService.getLatestTariffNews(null, 10);

        System.out.println("Total articles: " + response.getTotalResults());

        for (NewsArticle article : response.getArticles()) {
            System.out.println("Title: " + article.getTitle());
            System.out.println("Source: " + article.getSourceId());
            System.out.println("Published: " + article.getPubDate());
            System.out.println("---");
        }
    }
}
```

### Example 2: Advanced Search with NewsDataClient

```java
@Service
public class CustomNewsService {

    private final NewsDataClient newsDataClient;

    public CustomNewsService(NewsDataClient newsDataClient) {
        this.newsDataClient = newsDataClient;
    }

    public NewsDataResponse searchChinaTariffNews() {
        // Build custom request
        NewsDataRequest request = NewsDataRequest.builder()
            .query("tariff OR trade OR customs")
            .country(Arrays.asList("cn", "us"))
            .category(Arrays.asList("business", "politics"))
            .language(Arrays.asList("en"))
            .sentiment("negative")  // Only negative sentiment (paid plan)
            .priorityDomain("top")  // Top news sources only
            .removeDuplicate(true)
            .size(20)
            .build();

        return newsDataClient.getLatestNews(request);
    }
}
```

### Example 3: Frontend Integration (React)

```javascript
// src/services/newsApi.js
import api from "./api";

export const newsApi = {
  // Get latest tariff news
  getLatestTariffNews: async (country = null, limit = 10) => {
    const params = new URLSearchParams();
    if (country) params.append("country", country);
    params.append("limit", limit);

    const response = await api.get(`/api/news/tariff?${params}`);
    return response.data;
  },

  // Search news
  searchNews: async (query, country = null, limit = 10) => {
    const params = new URLSearchParams({ query, limit });
    if (country) params.append("country", country);

    const response = await api.get(`/api/news/search?${params}`);
    return response.data;
  },

  // Get next page
  getNextPage: async (nextPageToken) => {
    const response = await api.get(`/api/news/next?nextPage=${nextPageToken}`);
    return response.data;
  },
};

// Usage in component
import React, { useState, useEffect } from "react";
import { newsApi } from "../services/newsApi";

function TariffNewsPage() {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchNews = async () => {
      try {
        const data = await newsApi.getLatestTariffNews("us", 15);
        setNews(data.articles);
      } catch (error) {
        console.error("Error fetching news:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchNews();
  }, []);

  if (loading) return <div>Loading news...</div>;

  return (
    <div className="tariff-news">
      <h1>Latest Tariff News</h1>
      {news.map((article) => (
        <div key={article.articleId} className="news-card">
          <h2>{article.title}</h2>
          <p>{article.description}</p>
          <a href={article.link} target="_blank" rel="noopener noreferrer">
            Read more
          </a>
          <span className="source">{article.sourceId}</span>
          <span className="date">{article.pubDate}</span>
        </div>
      ))}
    </div>
  );
}
```

## NewsData.io Plans & Pricing

| Feature             | Free    | Basic ($199.99/mo) | Professional ($349.99/mo) | Corporate ($1299.99/mo) |
| ------------------- | ------- | ------------------ | ------------------------- | ----------------------- |
| API Credits         | 200/day | 20,000/month       | 50,000/month              | 1,000,000/month         |
| Articles per credit | 10      | 50                 | 50                        | 50                      |
| Historical data     | None    | 6 months           | 2 years                   | 5 years                 |
| AI Summary          | ❌      | ✅                 | ✅                        | ✅                      |
| Sentiment Analysis  | ❌      | ❌                 | ✅                        | ✅                      |
| AI Tags             | ❌      | ❌                 | ✅                        | ✅                      |
| AI Content          | ❌      | ❌                 | ✅                        | ✅                      |
| Full Content        | ❌      | ✅                 | ✅                        | ✅                      |

**Recommendation**: Start with the **Free plan** (200 articles/day) for testing. Upgrade to **Basic** ($199.99/mo) if you need 1,000 articles/day and 6 months of historical data.

## Rate Limits

- **Free plan**: 30 credits every 15 minutes
- **Paid plans**: 1,800 credits every 15 minutes

If you exceed the rate limit, you'll receive a `429 Too Many Requests` error. Wait 15 minutes before making more requests.

## Country Codes

Common country codes for tariff news:
| Code | Country |
|------|---------|
| `us` | United States |
| `cn` | China |
| `gb` | United Kingdom |
| `jp` | Japan |
| `de` | Germany |
| `fr` | France |
| `ca` | Canada |
| `au` | Australia |
| `in` | India |
| `mx` | Mexico |

Full list: [NewsData.io Country Codes](https://newsdata.io/news-sources/countries)

## Category Codes

Relevant categories for tariff news:

- `business` - Business and financial news
- `politics` - Political news and policy changes
- `economy` - Economic indicators and analysis
- `world` - International news

Full list: [NewsData.io Categories](https://newsdata.io/news-sources/categories)

## Error Handling

The NewsData API returns error responses in this format:

```json
{
  "status": "error",
  "code": "UnAuthorized",
  "message": "Your API key is invalid"
}
```

Common error codes:
| Code | HTTP Status | Meaning | Solution |
|------|-------------|---------|----------|
| `ParameterMissing` | 400 | Required parameter missing | Check request parameters |
| `UnAuthorized` | 401 | Invalid API key | Verify API key in application.yml |
| `IPRestricted` | 403 | IP not whitelisted | Check CORS settings |
| `ParameterDuplicate` | 409 | Duplicate parameter | Remove duplicate parameters |
| `RateLimitExceeded` | 429 | Too many requests | Wait 15 minutes |
| `InternalServerError` | 500 | Server error | Try again later |

## Troubleshooting

### Issue: "NewsData.io API key is not configured"

**Solution**: Make sure you've added your API key to `application.yml`:

```yaml
newsdata:
  api:
    key: YOUR_ACTUAL_API_KEY_HERE # Replace with real key
```

Then restart your Spring Boot application.

### Issue: "Rate Limit Exceeded"

**Solution**: You've made too many requests. Wait 15 minutes or upgrade to a paid plan for higher rate limits.

### Issue: "Archive endpoint returns error"

**Solution**: Historical news archive requires a **paid plan**. The free plan only supports the latest news (past 48 hours).

### Issue: No results returned

**Solution**:

1. Check your search query - try broader terms
2. Remove strict filters (country, category) to see if results exist
3. Verify the time period (past 48 hours for latest endpoint)

## Architecture Overview

```
NewsController (REST API)
    ↓
NewsService (Business Logic)
    ↓
NewsDataClient (HTTP Client)
    ↓
NewsData.io API
```

**Files Created**:

- `NewsDataClient.java` - HTTP client for NewsData.io API
- `NewsDataRequest.java` - Request parameter builder
- `NewsDataResponse.java` - Response wrapper
- `NewsArticle.java` - Article model
- `NewsSource.java` - News source model
- `NewsService.java` - Business logic layer
- `NewsController.java` - REST API endpoints

## Next Steps

1. ✅ **Get API Key**: Sign up at [newsdata.io/register](https://newsdata.io/register)
2. ✅ **Configure**: Add API key to `application.yml`
3. ✅ **Test**: Use Swagger UI at `http://localhost:8080/swagger-ui/index.html`
4. ✅ **Integrate**: Build frontend components using the React example above
5. ✅ **Monitor**: Track your API usage on the NewsData.io dashboard

## Support & Resources

- 📖 [NewsData.io Official Documentation](https://newsdata.io/documentation)
- 🎫 [NewsData.io Support](https://newsdata.io/contact)
- 🐛 [Report Issues](https://github.com/your-repo/issues)
- 💬 [Community](https://newsdata.io/blog)

## License

This integration is part of the TariffSheriff project and follows the same license terms.
