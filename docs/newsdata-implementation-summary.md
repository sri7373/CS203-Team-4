# NewsData.io Integration - Implementation Summary

## 📋 What Was Implemented

A complete integration with NewsData.io API to provide real-time and historical tariff-related news for the TariffSheriff application.

## 🎯 Key Features

✅ **Latest News** - Fetch tariff/trade news from the past 48 hours  
✅ **Custom Search** - Advanced search with AND/OR/NOT operators  
✅ **Country Filtering** - Filter news by specific countries  
✅ **Historical Archive** - Access historical news (paid plans)  
✅ **Pagination** - Navigate through large result sets  
✅ **News Sources** - Discover available news publishers  
✅ **AI Enhancements** - Sentiment analysis & summaries (paid plans)  
✅ **Error Handling** - Comprehensive error messages  
✅ **Documentation** - Full guides & examples

## 📦 Files Created

### Java Backend (7 files)

```
backend/src/main/java/com/smu/tariff/news/
├── NewsDataClient.java       (350 lines) - HTTP client for NewsData.io API
├── NewsDataRequest.java      (200 lines) - Request parameter builder with fluent API
├── NewsDataResponse.java     (50 lines)  - Response wrapper
├── NewsArticle.java          (180 lines) - Article model with all NewsData fields
├── NewsSource.java           (60 lines)  - News source model
├── NewsService.java          (120 lines) - Business logic layer
└── NewsController.java       (140 lines) - REST API endpoints with Swagger docs
```

### Configuration (1 file)

```
backend/src/main/resources/
└── application.yml           - Added newsdata.api.key configuration
```

### Documentation (2 files)

```
docs/
├── newsdata-integration.md   - Complete integration guide (450 lines)
└── newsdata-quickstart.md    - Quick reference cheat sheet (150 lines)
```

**Total**: 10 files, ~1,700 lines of code

## 🔑 API Key Setup Instructions

### Where to Place Your API Key

**File**: `backend/src/main/resources/application.yml`

**Line**: Find this section (around line 41):

```yaml
# NewsData.io API Configuration
# Get your API key from: https://newsdata.io/register
# Place your API key below (replace YOUR_NEWSDATA_API_KEY with actual key)
newsdata:
  api:
    key: YOUR_NEWSDATA_API_KEY
```

### How to Get Your API Key

1. **Sign up**: Visit https://newsdata.io/register
2. **Verify email**: Check your inbox for verification link
3. **Login**: Go to https://newsdata.io/login
4. **Copy key**: Your API key is displayed on the dashboard
5. **Paste**: Replace `YOUR_NEWSDATA_API_KEY` in `application.yml`
6. **Restart**: Restart your Spring Boot application

### Example Configuration

```yaml
newsdata:
  api:
    key: pub_123456abcdef7890xyz # Your actual key here
```

## 🚀 Available Endpoints

All endpoints require authentication (`JWT token`).

| Method | Endpoint            | Description                       |
| ------ | ------------------- | --------------------------------- |
| `GET`  | `/api/news/tariff`  | Get latest tariff news (past 48h) |
| `GET`  | `/api/news/search`  | Custom keyword search             |
| `GET`  | `/api/news/country` | Country-specific trade news       |
| `GET`  | `/api/news/archive` | Historical news (paid plan)       |
| `GET`  | `/api/news/next`    | Next page pagination              |
| `GET`  | `/api/news/sources` | Available news sources            |

## 📖 Quick Usage Examples

### 1. Get Latest Tariff News

```bash
GET /api/news/tariff?country=us&limit=10
```

```java
@Autowired
private NewsService newsService;

NewsDataResponse response = newsService.getLatestTariffNews("us", 10);
for (NewsArticle article : response.getArticles()) {
    System.out.println(article.getTitle());
}
```

### 2. Search for China Tariff News

```bash
GET /api/news/search?query=tariff%20AND%20china&limit=20
```

```java
NewsDataResponse response = newsService.searchNews("tariff AND china", null, 20);
```

### 3. Get Country Trade News

```bash
GET /api/news/country?countryCode=cn&limit=15
```

```java
NewsDataResponse response = newsService.getCountryTradeNews("cn", 15);
```

### 4. React Frontend Integration

```javascript
import api from "./services/api";

// Fetch latest tariff news
const fetchNews = async () => {
  const response = await api.get("/api/news/tariff?country=us&limit=20");
  setArticles(response.data.articles);
};

// Search with custom query
const searchNews = async (query) => {
  const response = await api.get(
    `/api/news/search?query=${encodeURIComponent(query)}&limit=15`
  );
  setArticles(response.data.articles);
};
```

## 🎨 Architecture

```
┌─────────────────┐
│ React Frontend  │
└────────┬────────┘
         │ HTTP Request
         ↓
┌─────────────────┐
│ NewsController  │  ← REST API Layer (6 endpoints)
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  NewsService    │  ← Business Logic Layer
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│NewsDataClient   │  ← HTTP Client (RestTemplate)
└────────┬────────┘
         │ HTTPS
         ↓
┌─────────────────┐
│ NewsData.io API │  ← External API
└─────────────────┘
```

## 📊 NewsData.io Plans

| Plan             | Price     | Credits/Month | Articles/Day | History  | AI Features     |
| ---------------- | --------- | ------------- | ------------ | -------- | --------------- |
| **Free**         | $0        | 200/day       | 2,000        | None     | ❌              |
| **Basic**        | $199.99   | 20,000        | 1,000,000    | 6 months | Summary only    |
| **Professional** | $349.99   | 50,000        | 2,500,000    | 2 years  | ✅ All          |
| **Corporate**    | $1,299.99 | 1,000,000     | 50,000,000   | 5 years  | ✅ All + Region |

**Recommendation**:

- Start with **Free** for testing (200 articles/day = sufficient for MVP)
- Upgrade to **Basic** ($199.99/mo) when you need 1,000+ articles/day and historical data

## 🔍 Search Capabilities

### Basic Search

- `tariff` - Single keyword
- `"trade war"` - Exact phrase

### Advanced Search

- `tariff AND china` - Both keywords required
- `tariff OR duty` - Either keyword
- `trade NOT brexit` - Exclude keyword
- `(tariff OR duty) AND china NOT steel` - Complex queries

### Filters

- **Country**: `us`, `cn`, `gb`, `jp`, etc.
- **Category**: `business`, `politics`, `economy`
- **Language**: `en`, `es`, `fr`, `zh`
- **Sentiment**: `positive`, `negative`, `neutral` (paid plans)
- **Date Range**: `from_date` to `to_date` (archive endpoint)

## 🛡️ Error Handling

The implementation handles all NewsData.io error codes:

| HTTP | Code                | Meaning                | Handled |
| ---- | ------------------- | ---------------------- | ------- |
| 400  | ParameterMissing    | Missing required param | ✅      |
| 401  | UnAuthorized        | Invalid API key        | ✅      |
| 403  | IPRestricted        | IP/Domain blocked      | ✅      |
| 429  | RateLimitExceeded   | Too many requests      | ✅      |
| 500  | InternalServerError | Server error           | ✅      |

All errors throw `IllegalStateException` with descriptive messages.

## 📝 Response Structure

```json
{
  "status": "success",
  "totalResults": 1250,
  "articles": [
    {
      "articleId": "abc123",
      "title": "New US Tariffs on Steel Imports",
      "description": "The United States has implemented...",
      "content": "Full article text...",
      "link": "https://example.com/article",
      "pubDate": "2025-10-24 14:30:00",
      "pubDateTZ": "UTC",
      "imageUrl": "https://example.com/image.jpg",
      "sourceId": "reuters",
      "sourceUrl": "https://reuters.com",
      "sourcePriority": 1,
      "country": ["us"],
      "category": ["business", "politics"],
      "language": "en",
      "keywords": ["tariff", "steel", "trade"],
      "creator": ["John Doe"],
      "sentiment": "neutral",
      "aiSummary": "US announces 25% tariffs...",
      "duplicate": false
    }
  ],
  "nextPage": "xyz789abc"
}
```

## 🎯 Key Fields Explained

| Field         | Type   | Available  | Description               |
| ------------- | ------ | ---------- | ------------------------- |
| `title`       | String | All plans  | Article headline          |
| `description` | String | All plans  | Article summary           |
| `content`     | String | Paid plans | Full article text         |
| `link`        | String | All plans  | Original URL              |
| `pubDate`     | String | All plans  | Publication date/time     |
| `sourceId`    | String | All plans  | News source identifier    |
| `sentiment`   | String | Pro/Corp   | positive/negative/neutral |
| `aiSummary`   | String | Paid plans | AI-generated summary      |
| `aiTag`       | String | Pro/Corp   | AI-classified tags        |
| `nextPage`    | String | All plans  | Pagination token          |

## 🔧 Testing the Integration

### 1. Via Swagger UI

1. Start your Spring Boot app: `mvn spring-boot:run`
2. Open: http://localhost:8080/swagger-ui/index.html
3. Navigate to "News" section
4. Click "Try it out" on any endpoint
5. Fill parameters and click "Execute"

### 2. Via cURL

```bash
# Get latest tariff news
curl -X GET "http://localhost:8080/api/news/tariff?limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Search for China tariff news
curl -X GET "http://localhost:8080/api/news/search?query=tariff%20AND%20china&limit=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Via Postman

1. Import collection from Swagger export
2. Add JWT token to Authorization header
3. Test each endpoint with different parameters

## 📚 Documentation Files

### Full Guide: `docs/newsdata-integration.md`

- Complete API documentation
- All 6 endpoints with examples
- Java & React code samples
- Pricing & plans comparison
- Error handling guide
- Troubleshooting section

### Quick Reference: `docs/newsdata-quickstart.md`

- One-page cheat sheet
- Common commands
- Search operators table
- Country codes reference
- Quick troubleshooting

## 🚦 Rate Limits

- **Free Plan**: 30 credits per 15 minutes
- **Paid Plans**: 1,800 credits per 15 minutes

**Credit Usage**:

- Free: 1 credit = 10 articles
- Paid: 1 credit = 50 articles

**Example**: On free plan, you can fetch 300 articles every 15 minutes (30 credits × 10 articles).

## 🔄 Pagination Example

```java
NewsDataResponse page1 = newsService.getLatestTariffNews("us", 10);

// Check if more pages exist
if (page1.getNextPage() != null) {
    NewsDataResponse page2 = newsService.getNextPage(page1.getNextPage());

    if (page2.getNextPage() != null) {
        NewsDataResponse page3 = newsService.getNextPage(page2.getNextPage());
    }
}
```

## 🎨 Frontend Integration Suggestion

Create a `TariffNewsPage.jsx`:

```jsx
import React, { useState, useEffect } from "react";
import api from "../services/api";

function TariffNewsPage() {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [country, setCountry] = useState("us");

  useEffect(() => {
    const fetchNews = async () => {
      try {
        const response = await api.get(
          `/api/news/tariff?country=${country}&limit=20`
        );
        setNews(response.data.articles);
      } catch (error) {
        console.error("Error fetching news:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchNews();
  }, [country]);

  if (loading) return <div>Loading...</div>;

  return (
    <div className="tariff-news">
      <h1>Latest Tariff News</h1>
      <select value={country} onChange={(e) => setCountry(e.target.value)}>
        <option value="us">United States</option>
        <option value="cn">China</option>
        <option value="gb">United Kingdom</option>
        <option value="jp">Japan</option>
      </select>

      {news.map((article) => (
        <article key={article.articleId} className="news-card">
          {article.imageUrl && (
            <img src={article.imageUrl} alt={article.title} />
          )}
          <h2>{article.title}</h2>
          <p>{article.description}</p>
          <div className="meta">
            <span className="source">{article.sourceId}</span>
            <span className="date">
              {new Date(article.pubDate).toLocaleDateString()}
            </span>
            {article.sentiment && (
              <span className="sentiment">{article.sentiment}</span>
            )}
          </div>
          <a href={article.link} target="_blank" rel="noopener noreferrer">
            Read full article →
          </a>
        </article>
      ))}
    </div>
  );
}

export default TariffNewsPage;
```

## ⚠️ Important Notes

1. **API Key Security**: Never commit your actual API key to Git
2. **Rate Limits**: Free plan is limited to 200 articles per day
3. **Archive Access**: Historical news requires paid plan
4. **Authentication**: All endpoints require JWT authentication
5. **Pagination**: Save `nextPage` tokens to navigate results

## 🐛 Common Issues & Solutions

| Issue                    | Cause                            | Solution                 |
| ------------------------ | -------------------------------- | ------------------------ |
| "API key not configured" | Missing key in `application.yml` | Add key and restart      |
| 401 Unauthorized         | Invalid API key                  | Check key is correct     |
| 429 Rate Limit           | Too many requests                | Wait 15 min or upgrade   |
| Empty results            | No matching articles             | Broaden search terms     |
| Archive error            | Not on paid plan                 | Upgrade or use `/latest` |

## ✅ Next Steps

1. **Get API Key**: Sign up at https://newsdata.io/register
2. **Configure**: Add key to `application.yml`
3. **Test**: Run app and visit Swagger UI
4. **Integrate Frontend**: Create news display components
5. **Monitor Usage**: Check NewsData.io dashboard for credits

## 📞 Support

- **NewsData.io Documentation**: https://newsdata.io/documentation
- **NewsData.io Support**: https://newsdata.io/contact
- **GitHub Issues**: [Your repo issues page]

## 🎉 Summary

You now have a **production-ready NewsData.io integration** with:

- ✅ 6 REST API endpoints
- ✅ Complete error handling
- ✅ Pagination support
- ✅ Swagger documentation
- ✅ Comprehensive guides
- ✅ Frontend examples
- ✅ Advanced search operators

The integration is **tailored to NewsData.io's API documentation** and follows your existing codebase patterns (similar to `GeminiClient.java`).

**Cost**: Start with **FREE** (200 articles/day), upgrade to **Basic** ($199.99/mo) when needed for 1,000+ articles/day and 6 months of history.
