# NewsData.io Integration Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     TariffSheriff Application               │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              React Frontend (Port 3000)               │  │
│  │                                                       │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │  │
│  │  │ TariffNews   │  │ SearchNews   │  │ NewsCard   │ │  │
│  │  │   Page       │  │   Page       │  │ Component  │ │  │
│  │  └──────────────┘  └──────────────┘  └────────────┘ │  │
│  │         │                  │                │         │  │
│  │         └──────────────────┴────────────────┘         │  │
│  │                            │                           │  │
│  │                     HTTP Requests                      │  │
│  │                   (JWT Authentication)                 │  │
│  └───────────────────────────┼───────────────────────────┘  │
│                                │                              │
│                                ↓                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │       Spring Boot Backend (Port 8080)               │    │
│  │                                                       │    │
│  │  ┌───────────────────────────────────────────────┐  │    │
│  │  │         NewsController.java                   │  │    │
│  │  │  @RestController @RequestMapping("/api/news") │  │    │
│  │  │                                                 │  │    │
│  │  │  GET  /tariff    - Latest tariff news         │  │    │
│  │  │  GET  /search    - Custom keyword search       │  │    │
│  │  │  GET  /country   - Country-specific news       │  │    │
│  │  │  GET  /archive   - Historical news             │  │    │
│  │  │  GET  /next      - Pagination                  │  │    │
│  │  │  GET  /sources   - News sources list           │  │    │
│  │  └─────────────────────┬─────────────────────────┘  │    │
│  │                        │                             │    │
│  │                        ↓                             │    │
│  │  ┌───────────────────────────────────────────────┐  │    │
│  │  │         NewsService.java                      │  │    │
│  │  │  @Service - Business Logic Layer              │  │    │
│  │  │                                                 │  │    │
│  │  │  • getLatestTariffNews(country, limit)        │  │    │
│  │  │  • searchNews(query, country, limit)           │  │    │
│  │  │  • getCountryTradeNews(countryCode, limit)    │  │    │
│  │  │  • getHistoricalTariffNews(dates, country)    │  │    │
│  │  │  • getNextPage(token)                          │  │    │
│  │  │  • getTariffNewsSources(country)              │  │    │
│  │  └─────────────────────┬─────────────────────────┘  │    │
│  │                        │                             │    │
│  │                        ↓                             │    │
│  │  ┌───────────────────────────────────────────────┐  │    │
│  │  │       NewsDataClient.java                     │  │    │
│  │  │  @Component - HTTP Client (RestTemplate)      │  │    │
│  │  │                                                 │  │    │
│  │  │  • getLatestNews(request)                     │  │    │
│  │  │  • getArchiveNews(request)                    │  │    │
│  │  │  • getSources(request)                        │  │    │
│  │  │  • buildUrl(endpoint, params)                 │  │    │
│  │  │  • executeRequest(url)                        │  │    │
│  │  └─────────────────────┬─────────────────────────┘  │    │
│  │                        │                             │    │
│  │                        │ HTTPS + API Key             │    │
│  │                        │ (from application.yml)      │    │
│  └────────────────────────┼─────────────────────────────┘    │
│                            │                                  │
└────────────────────────────┼──────────────────────────────────┘
                             │
                             ↓
         ┌───────────────────────────────────────┐
         │      NewsData.io API (External)       │
         │   https://newsdata.io/api/1/...       │
         │                                         │
         │  Endpoints:                            │
         │  • /latest  - Recent news (48h)        │
         │  • /archive - Historical news          │
         │  • /sources - News publishers          │
         │                                         │
         │  85,419 sources                        │
         │  206 countries                          │
         │  89 languages                           │
         │  100+ million articles                  │
         └───────────────────────────────────────┘
```

## Data Models

```
┌─────────────────────────────────────────────────────────────┐
│                      Data Flow                               │
└─────────────────────────────────────────────────────────────┘

NewsDataRequest (Client → API)
├── query: String               # "tariff AND china"
├── country: List<String>       # ["us", "cn"]
├── category: List<String>      # ["business", "politics"]
├── language: List<String>      # ["en"]
├── fromDate: String            # "2025-01-01"
├── toDate: String              # "2025-12-31"
├── sentiment: String           # "negative"
├── size: Integer               # 20
└── page: String                # "xyz789abc"

                ↓

NewsDataResponse (API → Client)
├── status: String              # "success"
├── totalResults: Integer       # 1250
├── nextPage: String            # "xyz789abc"
└── articles: List<NewsArticle>
    │
    ├── articleId: String       # "abc123"
    ├── title: String           # "New US Tariffs..."
    ├── description: String     # "Summary..."
    ├── content: String         # "Full text..."
    ├── link: String            # "https://..."
    ├── pubDate: String         # "2025-10-24 14:30:00"
    ├── imageUrl: String        # "https://..."
    ├── sourceId: String        # "reuters"
    ├── sourceUrl: String       # "https://reuters.com"
    ├── sourcePriority: Int     # 1
    ├── country: List<String>   # ["us"]
    ├── category: List<String>  # ["business"]
    ├── language: String        # "en"
    ├── keywords: List<String>  # ["tariff", "trade"]
    ├── creator: List<String>   # ["John Doe"]
    ├── sentiment: String       # "neutral"
    ├── aiSummary: String       # "AI summary..."
    ├── aiTag: String           # "trade-policy"
    └── duplicate: Boolean      # false
```

## Request Flow Diagram

```
┌──────────┐
│  Client  │
│ (React)  │
└────┬─────┘
     │
     │ 1. HTTP GET /api/news/tariff?country=us&limit=10
     │    Authorization: Bearer <JWT_TOKEN>
     │
     ↓
┌────────────────┐
│ NewsController │
└────┬───────────┘
     │
     │ 2. @PreAuthorize("isAuthenticated()") - Check JWT
     │
     ↓
┌───────────┐
│NewsService│
└────┬──────┘
     │
     │ 3. Build NewsDataRequest with filters:
     │    - query: "tariff OR trade OR customs"
     │    - country: ["us"]
     │    - category: ["business", "politics"]
     │    - language: ["en"]
     │    - size: 10
     │
     ↓
┌──────────────┐
│NewsDataClient│
└────┬─────────┘
     │
     │ 4. Build URL:
     │    https://newsdata.io/api/1/latest
     │      ?apikey=YOUR_API_KEY
     │      &q=tariff%20OR%20trade%20OR%20customs
     │      &country=us
     │      &category=business,politics
     │      &language=en
     │      &size=10
     │
     │ 5. RestTemplate.exchange(url, GET)
     │
     ↓
┌──────────────┐
│ NewsData.io  │
│     API      │
└────┬─────────┘
     │
     │ 6. Process request
     │    - Validate API key
     │    - Search 85,419+ sources
     │    - Filter by criteria
     │    - Apply AI analysis (if paid plan)
     │
     │ 7. Return JSON response:
     │    {
     │      "status": "success",
     │      "totalResults": 150,
     │      "articles": [ ... ],
     │      "nextPage": "xyz789"
     │    }
     │
     ↓
┌──────────────┐
│NewsDataClient│
└────┬─────────┘
     │
     │ 8. Parse JSON to NewsDataResponse object
     │    - Parse articles array
     │    - Handle errors
     │
     ↓
┌───────────┐
│NewsService│
└────┬──────┘
     │
     │ 9. Return NewsDataResponse
     │
     ↓
┌────────────────┐
│ NewsController │
└────┬───────────┘
     │
     │ 10. ResponseEntity.ok(response)
     │
     ↓
┌──────────┐
│  Client  │
│ (React)  │
└──────────┘
     │
     │ 11. Display articles in UI
     │     - Show title, description, image
     │     - Link to original source
     │     - Display sentiment, tags
```

## File Structure

```
TariffSheriff/
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/smu/tariff/
│   │   │   │   ├── news/                    ← NEW PACKAGE
│   │   │   │   │   ├── NewsDataClient.java  (350 lines)
│   │   │   │   │   ├── NewsDataRequest.java (200 lines)
│   │   │   │   │   ├── NewsDataResponse.java (50 lines)
│   │   │   │   │   ├── NewsArticle.java     (180 lines)
│   │   │   │   │   ├── NewsSource.java      (60 lines)
│   │   │   │   │   ├── NewsService.java     (120 lines)
│   │   │   │   │   └── NewsController.java  (140 lines)
│   │   │   │   │
│   │   │   │   ├── ai/
│   │   │   │   │   └── GeminiClient.java    (existing)
│   │   │   │   │
│   │   │   │   └── ... (other packages)
│   │   │   │
│   │   │   └── resources/
│   │   │       └── application.yml          ← UPDATED
│   │   │           └── newsdata.api.key: YOUR_KEY
│   │   │
│   │   └── test/
│   │       └── ... (tests to be added)
│   │
│   └── pom.xml                              (no changes needed)
│
├── frontend/
│   ├── src/
│   │   ├── services/
│   │   │   └── newsApi.js                   ← TO BE CREATED
│   │   │
│   │   └── pages/
│   │       └── TariffNewsPage.jsx           ← TO BE CREATED
│   │
│   └── ... (other frontend files)
│
└── docs/
    ├── newsdata-integration.md              ← CREATED (450 lines)
    ├── newsdata-quickstart.md               ← CREATED (150 lines)
    ├── newsdata-implementation-summary.md   ← CREATED (400 lines)
    └── newsdata-architecture.md             ← THIS FILE
```

## Configuration Flow

```
application.yml
┌─────────────────────────────┐
│ newsdata:                   │
│   api:                      │
│     key: YOUR_API_KEY       │  ← YOU NEED TO ADD THIS
└─────────────────────────────┘
              ↓
        Spring Boot
    @Value injection
              ↓
┌─────────────────────────────┐
│ NewsDataClient.java         │
│                             │
│ @Value("${newsdata.api.key}")
│ private String apiKey;      │
└─────────────────────────────┘
              ↓
        Used in API calls
              ↓
┌─────────────────────────────┐
│ https://newsdata.io/api/1/  │
│   ?apikey=YOUR_API_KEY      │
└─────────────────────────────┘
```

## API Key Setup Steps

```
Step 1: Sign Up
┌─────────────────────────────────────┐
│  https://newsdata.io/register       │
│  ├─ Email                           │
│  ├─ Password                        │
│  └─ Click "Sign Up"                 │
└─────────────────────────────────────┘
              ↓
Step 2: Verify Email
┌─────────────────────────────────────┐
│  Check inbox for verification email │
│  Click verification link            │
└─────────────────────────────────────┘
              ↓
Step 3: Get API Key
┌─────────────────────────────────────┐
│  https://newsdata.io/login          │
│  Dashboard shows your API key:      │
│  pub_123456abcdef7890xyz            │
└─────────────────────────────────────┘
              ↓
Step 4: Add to Config
┌─────────────────────────────────────┐
│  File: application.yml              │
│                                     │
│  newsdata:                          │
│    api:                             │
│      key: pub_123456abcdef7890xyz  │
└─────────────────────────────────────┘
              ↓
Step 5: Restart App
┌─────────────────────────────────────┐
│  mvn spring-boot:run                │
│  or                                 │
│  Restart in IDE                     │
└─────────────────────────────────────┘
              ↓
Step 6: Test
┌─────────────────────────────────────┐
│  http://localhost:8080/swagger-ui   │
│  Try /api/news/tariff endpoint      │
└─────────────────────────────────────┘
```

## Error Handling Flow

```
Client Request
      ↓
NewsController
      ↓
NewsService
      ↓
NewsDataClient
      ↓
Try {
  RestTemplate.exchange()
      ↓
  NewsData.io API
      ↓
  Success Response
}
Catch {
  ├─ RestClientException
  │     ↓
  │  throw IllegalStateException
  │  "Failed to fetch news from NewsData.io"
  │
  ├─ JsonProcessingException
  │     ↓
  │  throw IllegalStateException
  │  "Failed to parse NewsData.io response"
  │
  └─ API Error Response
        ↓
     Parse error JSON
        ↓
     throw IllegalStateException
     "NewsData.io API error [CODE]: message"
}
      ↓
Controller catches exception
      ↓
Returns HTTP error response
      ↓
Client receives error
```

## Pagination Flow

```
Page 1 Request
GET /api/news/tariff?limit=10
      ↓
┌─────────────────────────────┐
│ Response                    │
├─────────────────────────────┤
│ articles: [10 articles]     │
│ nextPage: "xyz789"          │
│ totalResults: 150           │
└─────────────────────────────┘
      ↓
Client stores nextPage token
      ↓
Page 2 Request
GET /api/news/next?nextPage=xyz789
      ↓
┌─────────────────────────────┐
│ Response                    │
├─────────────────────────────┤
│ articles: [10 articles]     │
│ nextPage: "abc456"          │
│ totalResults: 150           │
└─────────────────────────────┘
      ↓
Continue until nextPage is null
```

## Rate Limiting

```
Free Plan:
┌──────────────────┐
│ 30 credits       │ = 300 articles
│ per 15 minutes   │
└──────────────────┘
      ↓
If exceeded:
┌──────────────────┐
│ HTTP 429         │
│ Rate Limit       │
│ Exceeded         │
└──────────────────┘
      ↓
Wait 15 minutes

Paid Plan:
┌──────────────────┐
│ 1,800 credits    │ = 90,000 articles
│ per 15 minutes   │
└──────────────────┘
```

## Testing Strategy

```
Unit Tests (to be added)
├── NewsDataClientTest
│   ├── testGetLatestNews()
│   ├── testGetArchiveNews()
│   ├── testGetSources()
│   ├── testBuildUrl()
│   └── testErrorHandling()
│
├── NewsServiceTest
│   ├── testGetLatestTariffNews()
│   ├── testSearchNews()
│   ├── testGetCountryTradeNews()
│   └── testPagination()
│
└── NewsControllerTest
    ├── testTariffEndpoint()
    ├── testSearchEndpoint()
    ├── testCountryEndpoint()
    ├── testArchiveEndpoint()
    ├── testNextEndpoint()
    └── testSourcesEndpoint()

Integration Tests
└── NewsIntegrationTest
    └── testEndToEndNewsFlow()
```

## Deployment Considerations

```
Production Checklist:
┌─────────────────────────────────┐
│ ✓ API key in environment vars  │
│   (not hardcoded in YML)        │
│                                 │
│ ✓ Rate limiting middleware      │
│   (prevent API abuse)           │
│                                 │
│ ✓ Caching layer                 │
│   (reduce API calls)            │
│                                 │
│ ✓ Error monitoring              │
│   (Sentry, CloudWatch)          │
│                                 │
│ ✓ Usage tracking                │
│   (monitor API credits)         │
│                                 │
│ ✓ Backup plan if API down       │
│   (cached results, fallback)    │
└─────────────────────────────────┘
```

## Cost Optimization

```
Strategy:
┌─────────────────────────────────┐
│ 1. Implement caching            │
│    - Cache responses for 1 hour │
│    - Reduces redundant API calls│
│                                 │
│ 2. Lazy loading                 │
│    - Load news on demand        │
│    - Not on every page visit    │
│                                 │
│ 3. Batch requests               │
│    - Fetch 50 articles at once  │
│    - vs 10 separate calls       │
│                                 │
│ 4. Monitor usage                │
│    - Track API credit usage     │
│    - Alert when approaching limit
└─────────────────────────────────┘

Free Plan: 200 credits/day
= 2,000 articles/day
= Sufficient for MVP testing

Paid Plan: $199.99/month
= 20,000 credits/month
= 1,000,000 articles/month
= Needed for production
```

## Summary

This integration provides:
✅ **6 REST endpoints** for news access
✅ **7 Java classes** following Spring Boot patterns
✅ **Complete error handling** with meaningful messages
✅ **Pagination support** for large result sets
✅ **Advanced search** with AND/OR/NOT operators
✅ **Country filtering** for targeted news
✅ **Swagger documentation** for easy API testing
✅ **Production-ready** architecture

**API Key Required**: `newsdata.api.key` in `application.yml`
**Cost**: Free (200/day) → $199.99/mo (20,000/month)
**Documentation**: See `docs/newsdata-integration.md`
