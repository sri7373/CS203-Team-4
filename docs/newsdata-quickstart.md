# NewsData.io Quick Reference

## 🔑 API Key Location

**File**: `backend/src/main/resources/application.yml`

**Section**:

```yaml
newsdata:
  api:
    key: YOUR_NEWSDATA_API_KEY # Replace with your key from newsdata.io/register
```

---

## 📌 Quick Start

1. **Sign up**: https://newsdata.io/register
2. **Copy API key** from dashboard
3. **Paste key** into `application.yml` (replace `YOUR_NEWSDATA_API_KEY`)
4. **Restart** Spring Boot application
5. **Test** at http://localhost:8080/swagger-ui/index.html

---

## 🚀 API Endpoints Cheat Sheet

| Endpoint                | Description                 | Key Params                               |
| ----------------------- | --------------------------- | ---------------------------------------- |
| `GET /api/news/tariff`  | Latest tariff news (48h)    | `country`, `limit`                       |
| `GET /api/news/search`  | Custom keyword search       | `query` (required), `country`, `limit`   |
| `GET /api/news/country` | Country-specific trade news | `countryCode` (required), `limit`        |
| `GET /api/news/archive` | Historical news (paid plan) | `fromDate`, `toDate`, `country`, `limit` |
| `GET /api/news/next`    | Next page pagination        | `nextPage` (token from response)         |
| `GET /api/news/sources` | Available news sources      | `country`                                |

---

## 🔍 Search Operators

| Syntax     | Example                      | Meaning                |
| ---------- | ---------------------------- | ---------------------- |
| `keyword`  | `tariff`                     | Articles with "tariff" |
| `"phrase"` | `"trade war"`                | Exact phrase           |
| `AND`      | `tariff AND china`           | Both keywords          |
| `OR`       | `tariff OR duty`             | Either keyword         |
| `NOT`      | `trade NOT brexit`           | Exclude keyword        |
| `()`       | `(tariff OR duty) AND china` | Grouping               |

---

## 🌍 Country Codes

| Code | Country       | Code | Country        |
| ---- | ------------- | ---- | -------------- |
| `us` | United States | `gb` | United Kingdom |
| `cn` | China         | `jp` | Japan          |
| `de` | Germany       | `fr` | France         |
| `ca` | Canada        | `au` | Australia      |
| `in` | India         | `mx` | Mexico         |

Full list: https://newsdata.io/news-sources/countries

---

## 📊 Plans & Limits

| Plan             | Price       | Credits      | Articles/day | History  |
| ---------------- | ----------- | ------------ | ------------ | -------- |
| **Free**         | $0          | 200/day      | 2,000        | None     |
| **Basic**        | $199.99/mo  | 20,000/mo    | 1,000,000    | 6 months |
| **Professional** | $349.99/mo  | 50,000/mo    | 2,500,000    | 2 years  |
| **Corporate**    | $1299.99/mo | 1,000,000/mo | 50,000,000   | 5 years  |

**Note**: 1 credit = 10 articles (free), 1 credit = 50 articles (paid)

---

## ⚡ Rate Limits

- **Free**: 30 credits / 15 minutes
- **Paid**: 1,800 credits / 15 minutes

---

## 🛠️ Common Code Snippets

### Get Latest Tariff News

```java
@Autowired
private NewsService newsService;

NewsDataResponse response = newsService.getLatestTariffNews("us", 10);
```

### Search with Custom Query

```java
NewsDataResponse response = newsService.searchNews("tariff AND china", null, 20);
```

### Build Custom Request

```java
@Autowired
private NewsDataClient newsDataClient;

NewsDataRequest request = NewsDataRequest.builder()
    .query("trade policy")
    .country(Arrays.asList("us", "cn"))
    .category(Arrays.asList("business"))
    .sentiment("negative")  // Paid plan only
    .size(15)
    .build();

NewsDataResponse response = newsDataClient.getLatestNews(request);
```

### React Frontend

```javascript
import api from "./api";

const response = await api.get("/api/news/tariff?country=us&limit=20");
const articles = response.data.articles;
```

---

## 🐛 Troubleshooting

| Error                    | Solution                                  |
| ------------------------ | ----------------------------------------- |
| "API key not configured" | Add key to `application.yml`, restart app |
| 429 Rate Limit           | Wait 15 minutes or upgrade plan           |
| 401 Unauthorized         | Check API key is correct                  |
| Archive error            | Archive requires paid plan                |
| No results               | Try broader search terms                  |

---

## 📦 Response Structure

```json
{
  "status": "success",
  "totalResults": 1250,
  "articles": [
    {
      "articleId": "abc123",
      "title": "Article title",
      "description": "Article description",
      "link": "https://...",
      "pubDate": "2025-10-24 14:30:00",
      "sourceId": "reuters",
      "country": ["us"],
      "category": ["business"],
      "sentiment": "neutral",
      "aiSummary": "AI summary..."
    }
  ],
  "nextPage": "xyz789" // Use for pagination
}
```

---

## 📚 Key Article Fields

| Field         | Type   | Description               | Plans    |
| ------------- | ------ | ------------------------- | -------- |
| `title`       | String | Article title             | All      |
| `description` | String | Article summary           | All      |
| `content`     | String | Full article text         | Paid     |
| `link`        | String | Original URL              | All      |
| `pubDate`     | String | Publication date          | All      |
| `sourceId`    | String | News source name          | All      |
| `sentiment`   | String | positive/negative/neutral | Pro/Corp |
| `aiSummary`   | String | AI-generated summary      | Paid     |
| `aiTag`       | String | AI classification         | Pro/Corp |

---

## 🔗 Links

- 📖 Docs: https://newsdata.io/documentation
- 🎫 Sign Up: https://newsdata.io/register
- 💬 Support: https://newsdata.io/contact
- 📊 Dashboard: https://newsdata.io/login

---

## 📋 Files Created

```
backend/src/main/java/com/smu/tariff/news/
├── NewsDataClient.java      # HTTP client
├── NewsDataRequest.java     # Request builder
├── NewsDataResponse.java    # Response wrapper
├── NewsArticle.java         # Article model
├── NewsSource.java          # Source model
├── NewsService.java         # Business logic
└── NewsController.java      # REST endpoints

backend/src/main/resources/
└── application.yml          # API key config

docs/
├── newsdata-integration.md  # Full documentation
└── newsdata-quickstart.md   # This file
```
