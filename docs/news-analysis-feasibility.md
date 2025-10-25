# News Analysis Feature - Feasibility Study

## Executive Summary

This document analyzes the feasibility of adding a news analysis feature to the TariffSheriff application based on Twitter/X data, with a focus on trade-related news that could impact tariff decisions and international trade.

**Recommendation**: A **hybrid approach** combining Grok AI for real-time data access with a RAG (Retrieval-Augmented Generation) system for historical analysis is the most robust solution.

---

## Current Application Context

### Existing Infrastructure

- **Backend**: Spring Boot 3.3.4 with Java 17
- **Database**: PostgreSQL on AWS RDS
- **AI Integration**: Google Gemini API (already integrated)
- **Security**: JWT-based authentication
- **Architecture**: RESTful API with React frontend

### Current AI Usage

Your app already uses Gemini AI for generating tariff calculation summaries, demonstrating:

- ✅ Experience with AI API integration
- ✅ Established patterns for external API calls
- ✅ Error handling and timeout management
- ✅ Environment-based configuration

---

## Option 1: Grok AI Integration

### Overview

Grok AI (by xAI) is designed with **real-time access to X (Twitter) data**, making it ideal for current events and social media trends. **The API is now publicly available** with OpenAI-compatible endpoints.

### Technical Feasibility

#### Pros

1. **✅ Real-time X/Twitter Access**: Native integration with X platform data built-in
2. **✅ Multi-Source Search**: Supports web, X posts, news, and RSS feeds
3. **✅ OpenAI Compatible**: Works with OpenAI SDK - easy migration
4. **✅ Advanced Filtering**: Filter by X handles, post engagement, date ranges
5. **✅ Citation Support**: Returns URLs to source articles/posts
6. **✅ Multiple Models**: Cost-efficient options (Grok-4-fast at $0.20/$0.50) or flagship (Grok-4 at $3/$15)
7. **✅ Similar to Existing Code**: REST API pattern matches your `GeminiClient`

#### Cons

1. **Live Search Cost**: $25 per 1,000 sources (sources = articles/posts retrieved)
   - Each source costs $0.025
   - This is **ON TOP** of model costs
2. **Rate Limits**: Standard API rate limits (need to implement backoff)
3. **New Service Dependency**: Another external service to monitor
4. **Live Search Deprecation**: Live Search API will be deprecated by **December 15, 2025** (migrating to agentic tool calling API)

#### Implementation Complexity: **Low-Medium**

- Very similar to your existing `GeminiClient` implementation
- OpenAI SDK compatibility means drop-in replacement possible
- Live Search is a simple parameter in the request
- Built-in X data access (no separate Twitter API needed!)

### Code Implementation Estimate

```java
@Component
public class GrokClient {
    private static final String GROK_API_URL = "https://api.x.ai/v1/chat/completions";
    private static final String MODEL = "grok-4-fast-reasoning"; // or grok-3-mini for cost savings

    // OpenAI-compatible API - very similar to GeminiClient
    // Estimated LOC: ~200-250 lines (includes search parameters)
    // Time to implement: 4-8 hours

    public GrokAnalysisResponse analyzeTradeNews(String query, SearchOptions options) {
        // Enable live search with X, web, and news sources
        // Filter by trade-related X handles, date ranges
        // Return analysis with citations
    }
}
```

### Real Code Example

```java
@Component
public class GrokClient {
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String analyzeWithLiveSearch(String prompt, List<String> xHandles) {
        String url = "https://api.x.ai/v1/chat/completions";

        Map<String, Object> searchParams = Map.of(
            "mode", "on",
            "return_citations", true,
            "sources", List.of(
                Map.of("type", "x",
                       "included_x_handles", xHandles,
                       "post_favorite_count", 100), // Only popular posts
                Map.of("type", "web"),
                Map.of("type", "news")
            )
        );

        Map<String, Object> requestBody = Map.of(
            "model", "grok-4-fast-reasoning",
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "search_parameters", searchParams
        );

        // Make request and parse response with citations
        // Response includes usage.num_sources_used for cost tracking
    }
}
```

### Cost Estimate (Updated with Real Pricing)

**Grok-4-fast-reasoning** (Recommended for your use case):

- **Text Input**: $0.20 per 1M tokens
- **Output**: $0.50 per 1M tokens
- **Live Search**: $25 per 1,000 sources (= $0.025 per source)

**Example Query Cost**:

- Input: 500 tokens = $0.0001
- Output: 1,000 tokens = $0.0005
- Live Search (10 sources): 10 × $0.025 = $0.25
- **Total per query: ~$0.25**

**Monthly estimates** (100 queries/day):

- Model costs: ~$2-5/month
- Live Search: ~$750/month (assuming 10 sources per query)
- **Total: ~$750-755/month**

**Cost-saving option** (Grok-3-mini):

- Text Input: $0.30 per 1M tokens
- Output: $0.50 per 1M tokens
- Same live search costs
- **Better for simple analysis, worse for complex reasoning**

### Risk Assessment: **LOW-MEDIUM** ✅ (Updated)

- ✅ API publicly available NOW
- ✅ Clear pricing model
- ✅ OpenAI SDK compatible
- ⚠️ Live Search will be deprecated Dec 2025 (migration path exists)
- ⚠️ Live Search costs can add up quickly
- ⚠️ Need to optimize source usage to control costs

---

## Option 2: RAG (Retrieval-Augmented Generation)

### Overview

RAG combines a vector database with your existing AI model (Gemini) to provide contextual, source-grounded news analysis.

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Data Ingestion Layer                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Twitter  │  │   RSS    │  │  News    │             │
│  │   API    │  │  Feeds   │  │   APIs   │             │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
│       │             │              │                    │
│       └─────────────┴──────────────┘                    │
│                     │                                   │
├─────────────────────┼───────────────────────────────────┤
│              Text Processing                            │
│         (Chunking, Cleaning, Metadata)                  │
│                     │                                   │
├─────────────────────┼───────────────────────────────────┤
│           Embedding Generation Layer                    │
│              (Vector Embeddings)                        │
│                     │                                   │
├─────────────────────┼───────────────────────────────────┤
│              Vector Database                            │
│          (PostgreSQL pgvector OR                        │
│           Pinecone/Weaviate/Qdrant)                     │
│                     │                                   │
├─────────────────────┼───────────────────────────────────┤
│               Query Processing                          │
│    ┌────────────────┴────────────────┐                 │
│    │   User Query → Embedding        │                 │
│    │   Semantic Search → Top K Docs  │                 │
│    └────────────────┬────────────────┘                 │
├─────────────────────┼───────────────────────────────────┤
│          LLM Context Enhancement                        │
│    ┌────────────────┴────────────────┐                 │
│    │   Retrieved Docs + Query        │                 │
│    │   → Gemini AI                   │                 │
│    │   → Contextual Answer           │                 │
│    └─────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────┘
```

### Technical Feasibility

#### Pros

1. **Multiple Data Sources**:
   - Twitter API
   - News APIs (NewsAPI, Google News, Bloomberg)
   - RSS feeds from trade publications
   - Government press releases
2. **Historical Analysis**: Can store and query months/years of data
3. **Source Attribution**: Every insight can be traced back to specific articles
4. **Cost-Effective**: Pay only for storage and embeddings
5. **Flexibility**: Easy to add/remove data sources
6. **Scalability**: Can handle large volumes of historical data
7. **Existing Infrastructure**: Can leverage your PostgreSQL database

#### Cons

1. **Implementation Complexity**: More components to build and maintain
2. **Data Pipeline**: Requires continuous data ingestion process
3. **Storage Costs**: Vector embeddings require significant storage
4. **Maintenance**: Need to manage data freshness and relevance
5. **Initial Setup Time**: Longer development cycle

#### Implementation Complexity: **High**

- Requires vector database setup
- Data ingestion pipelines
- Embedding generation
- Search and ranking algorithms

### Component Breakdown

#### 1. Vector Database Options

**Option A: PostgreSQL with pgvector** (RECOMMENDED)

```sql
-- Already using PostgreSQL - add pgvector extension
CREATE EXTENSION vector;

CREATE TABLE news_embeddings (
    id SERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    embedding vector(768),  -- Dimension based on embedding model
    source VARCHAR(100),
    published_at TIMESTAMP,
    url TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON news_embeddings USING ivfflat (embedding vector_cosine_ops);
```

**Pros**:

- ✅ No additional database service
- ✅ Familiar technology
- ✅ Cost-effective
- ✅ Easy backup and maintenance

**Cons**:

- ❌ Performance may be slower than specialized vector DBs at scale
- ❌ Limited vector search optimization

**Option B: Pinecone/Weaviate/Qdrant**

- Specialized vector databases with better performance
- Additional service to manage
- Monthly costs: $70-500+

#### 2. Data Sources

**Twitter/X API** (For Real-time Data)

```java
// Twitter API v2 Integration
@Service
public class TwitterDataIngestionService {
    // Fetch tweets about trade, tariffs, international relations
    // Filter by keywords: tariffs, trade war, import/export, sanctions
    // Store with metadata: author, engagement, timestamp
}
```

**Cost**: $100-5000/month depending on volume

**News APIs** (For Structured News)

- **NewsAPI**: $449/month for commercial use
- **Google News RSS**: Free (limited)
- **Bloomberg API**: Enterprise pricing
- **Reuters API**: Enterprise pricing

**Recommended Approach**: Start with free RSS feeds + selective premium APIs

#### 3. Embedding Generation

**Option A: OpenAI Embeddings API**

```java
@Service
public class EmbeddingService {
    // text-embedding-3-small: $0.02 per 1M tokens
    // text-embedding-3-large: $0.13 per 1M tokens

    public float[] generateEmbedding(String text) {
        // Call OpenAI embeddings API
        // Return 768 or 1536 dimensional vector
    }
}
```

**Cost**: ~$0.10 per 1000 articles

**Option B: Google Vertex AI Embeddings**

- Integrated with your existing Google Cloud setup
- textembedding-gecko: $0.025 per 1000 requests

**Option C: Open-Source Models** (Sentence Transformers)

```java
// Run locally or on AWS
// Models: all-MiniLM-L6-v2, all-mpnet-base-v2
// Free but requires infrastructure
```

#### 4. RAG Query Pipeline

```java
@Service
public class NewsAnalysisService {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorSearchRepository vectorSearchRepo;

    @Autowired
    private GeminiClient geminiClient;

    public NewsAnalysisResponse analyzeTradeNews(String query) {
        // 1. Generate query embedding
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        // 2. Semantic search for relevant articles
        List<NewsArticle> relevantArticles =
            vectorSearchRepo.findTopKSimilar(queryEmbedding, k=10);

        // 3. Build context for LLM
        String context = buildContext(relevantArticles);

        // 4. Generate analysis with Gemini
        String prompt = String.format(
            "Based on these news articles:\n%s\n\nAnalyze: %s",
            context, query
        );
        String analysis = geminiClient.generateSummary(prompt);

        // 5. Return with source attribution
        return new NewsAnalysisResponse(analysis, relevantArticles);
    }
}
```

### Cost Estimate (RAG)

- **Initial Development**: 3-4 weeks
- **Embedding API**: $10-50/month (for moderate volume)
- **Vector Database**:
  - pgvector: $0 (existing PostgreSQL)
  - Pinecone: $70-200/month
- **News Data APIs**: $0-500/month (depends on sources)
- **Twitter API**: $100-500/month (optional)
- **Infrastructure**: $20-100/month (additional compute)

**Total Estimated Monthly Cost**: $30-650/month

### Risk Assessment: **Medium**

- ✅ Proven technology stack
- ✅ Flexible architecture
- ⚠️ Moderate complexity
- ⚠️ Ongoing maintenance required

---

## Option 3: Hybrid Approach (RECOMMENDED)

### Overview

Combine Grok AI for real-time insights with RAG for historical analysis and verification.

### Architecture

```
User Query: "How will recent US-China trade tensions affect semiconductor tariffs?"
    │
    ├─→ Grok AI (Real-time Twitter Analysis)
    │   └─→ "Breaking: US announced new semiconductor export controls 2 hours ago"
    │
    └─→ RAG System (Historical Context)
        └─→ "Historical pattern: Similar announcements in 2019 led to 15% tariff increases"

Combined Response: Real-time news + Historical context + Source attribution
```

### Implementation Strategy

#### Phase 1: MVP with RAG (Weeks 1-4)

1. Set up pgvector in existing PostgreSQL
2. Create basic data ingestion pipeline (RSS feeds)
3. Implement embedding generation (OpenAI or Vertex AI)
4. Build semantic search API endpoint
5. Integrate with existing Gemini client

**Deliverables**:

- `/api/news/analyze` endpoint
- Basic historical news analysis
- Source attribution

#### Phase 2: Twitter Integration (Weeks 5-6)

1. Integrate Twitter API for data collection
2. Add Twitter-specific filtering and processing
3. Create scheduled jobs for data ingestion

**Deliverables**:

- Real-time tweet ingestion
- Twitter sentiment analysis
- Trending topics detection

#### Phase 3: Grok AI Integration (Future - when API available)

1. Add Grok client similar to Gemini
2. Implement dual-AI querying (Grok + Gemini)
3. Create confidence scoring system

**Deliverables**:

- Real-time Grok insights
- Hybrid response generation
- Comparison between AI models

### Technical Stack

```yaml
Backend Additions:
  - pgvector (PostgreSQL extension)
  - OpenAI Embeddings API or Vertex AI
  - Twitter API v2 client
  - Scheduled jobs (Spring @Scheduled)
  - Caching layer (Redis optional)

New Entities:
  - NewsArticle
  - NewsEmbedding
  - NewsQuery
  - NewsAnalysisResult

New Services:
  - EmbeddingService
  - VectorSearchService
  - NewsIngestionService
  - NewsAnalysisService
  - TwitterClientService
  - GrokClient (future)

New Controllers:
  - NewsAnalysisController
```

### Cost Estimate (Hybrid)

**Phase 1 (MVP)**: $30-100/month
**Phase 2 (Twitter)**: $130-600/month
**Phase 3 (Grok)**: TBD + $100-300/month estimated

### Risk Assessment: **Low to Medium**

- ✅ Incremental implementation
- ✅ Fallback options at each phase
- ✅ Proven technologies
- ⚠️ Moderate ongoing costs

---

## Comparative Analysis (UPDATED)

| Feature             | Grok AI Only                  | RAG Only                | Hybrid (Recommended) |
| ------------------- | ----------------------------- | ----------------------- | -------------------- |
| Real-time Data      | ✅ Excellent                  | ❌ Delayed              | ✅ Excellent         |
| Historical Analysis | ❌ Limited                    | ✅ Excellent            | ✅ Excellent         |
| Source Attribution  | ✅ Built-in citations         | ✅ Full                 | ✅ Full              |
| Cost (Monthly)      | **$750-2000**                 | $30-650                 | $800-2500            |
| Implementation Time | **1 week**                    | 3-4 weeks               | 4-6 weeks            |
| Maintenance         | Low                           | Medium                  | Medium-High          |
| API Availability    | **✅ Available NOW**          | ✅ Available now        | ✅ All available     |
| Scalability         | ⚠️ Cost scales with sources   | ✅ High                 | ✅ High              |
| Data Sources        | **X + Web + News (built-in)** | Multiple                | Multiple             |
| Risk Level          | **Low-Medium** ✅             | Medium                  | Low                  |
| X/Twitter Access    | **✅ Native (no extra API)**  | ❌ Requires Twitter API | ✅ Built-in          |
| Setup Complexity    | **✅ Very Simple**            | ❌ Complex              | ⚠️ Moderate          |

---

## Recommended Implementation Plan (REVISED)

### **NEW RECOMMENDATION: Start with Grok AI (Quick Win)**

Given that Grok API is now available with built-in X/Twitter access and multi-source search, the recommendation has changed:

### Phase 1: Grok AI MVP (Priority: HIGH) ⭐

**Timeline**: 1 week
**Cost**: $750-1000/month (assuming 100 queries/day with 10 sources each)

**Why Start with Grok**:

1. ✅ **Fastest time to market**: 1 week vs 4 weeks for RAG
2. ✅ **Built-in X/Twitter data**: No separate Twitter API needed
3. ✅ **OpenAI compatible**: Similar to your existing Gemini integration
4. ✅ **Multi-source search**: X + Web + News in one API call
5. ✅ **Citations included**: Automatic source attribution
6. ✅ **Real-time data**: Breaking news and trending topics
7. ✅ **Validate user interest quickly**: Test if users want this feature before building complex RAG

**Tasks**:

1. Create xAI account and generate API key
2. Implement `GrokClient` (similar to `GeminiClient`)
3. Create `NewsAnalysisService` with Grok integration
4. Build API endpoint: `POST /api/news/analyze`
5. Implement cost tracking (usage.num_sources_used)
6. Add rate limiting to control costs
7. Create frontend component for news analysis
8. Add monitoring and alerts for API costs

**Acceptance Criteria**:

- ✅ Can query real-time X posts, web, and news in single request
- ✅ Returns analysis with source citations (URLs)
- ✅ Filters by trade-related X handles
- ✅ Tracks cost per query
- ✅ Response time < 5 seconds
- ✅ Implements daily/monthly cost caps

**Cost Control Strategies**:

```java
@Service
public class NewsAnalysisService {
    private static final int MAX_SOURCES = 5; // Limit to control costs
    private static final int MAX_QUERIES_PER_USER_PER_DAY = 10;

    // Each query with 5 sources costs ~$0.125
    // 10 queries/user/day = $1.25/user/day max
}
```

### Phase 2: RAG for Historical Analysis (Priority: MEDIUM)

**Timeline**: 3-4 weeks
**Cost**: Additional $30-100/month

**When to implement**:

- ✅ After Grok MVP proves valuable
- ✅ When users need deeper historical analysis (3+ months back)
- ✅ When you want to reduce per-query costs for common queries

**Why Add RAG Later**:

1. Grok live search only retrieves recent data (typically 7-30 days)
2. RAG provides long-term historical context
3. Can cache common analyses to reduce Grok API calls
4. Provides redundancy if Grok API has issues

**Tasks**:

1. Set up pgvector extension in PostgreSQL
2. Create data ingestion pipeline (store articles from Grok citations)
3. Generate embeddings for stored articles
4. Implement semantic search
5. Build hybrid query system (RAG for historical + Grok for real-time)

**Architecture**:

```
User Query: "US-China trade tensions impact on semiconductors"
     |
     ├─→ Check if historical query (> 30 days ago)
     |   ├─→ YES: Use RAG system (cached, cheap)
     |   └─→ NO: Continue to Grok
     |
     └─→ Grok Live Search (real-time data, expensive)
         └─→ Store citations in RAG database (for future queries)
```

### Phase 3: Optimization (Priority: LOW)

**Timeline**: 1-2 weeks
**Cost**: Reduces costs by 30-50%

**Optimizations**:

1. **Smart Caching**: Cache Grok responses for 1-6 hours
2. **Query Classification**: Route simple queries to RAG, complex to Grok
3. **Batch Processing**: Collect queries and process in batches
4. **Source Optimization**: Start with fewer sources (3-5 instead of 10-20)
5. **Model Selection**: Use Grok-3-mini for simple queries ($0.30 vs $3 input)

### Frontend Integration

**New Page**: `/insights/news-analysis`

**Features**:

- Query input for trade-related questions
- Real-time analysis generation
- Source article list with links
- Timeline visualization of relevant news
- Sentiment trends over time
- Alert system for breaking news

---

## Alternative: Lightweight Quick Win

If you want a **faster, cheaper MVP** to test user interest:

### Simple News Aggregator + Gemini

**Timeline**: 1 week
**Cost**: $0-10/month

1. Use free RSS feeds from:
   - Reuters Trade News
   - Bloomberg Trade
   - WTO News
   - US Trade Representative
2. Store articles in PostgreSQL (no vectors)
3. Simple keyword search + Gemini summarization
4. No Twitter integration initially

**Pros**:

- ✅ Very fast to implement
- ✅ Minimal cost
- ✅ Tests user demand
- ✅ Can upgrade to RAG later

**Cons**:

- ❌ Basic search (no semantic understanding)
- ❌ No real-time social media data
- ❌ Limited analysis capabilities

---

## Security & Compliance Considerations

### Data Privacy

- ✅ Twitter data: Publicly available, ensure compliance with Twitter ToS
- ✅ News articles: Respect copyright, store only metadata + links
- ✅ User queries: Log for improvement but maintain privacy

### API Key Management

- ✅ Store all API keys in environment variables (not in code)
- ✅ Use Spring Boot's encryption for sensitive data
- ✅ Implement rate limiting to prevent abuse

### Rate Limiting

```java
@Service
public class RateLimitService {
    // Prevent API abuse
    // Max 10 news analysis queries per user per hour
    private static final int MAX_QUERIES_PER_HOUR = 10;
}
```

---

## Final Recommendation (UPDATED)

### **Start with: Grok AI MVP** ⭐

**Rationale**:

1. **Grok API is NOW AVAILABLE** ✅ - no waiting required
2. **Fastest implementation** - 1 week vs 4 weeks for RAG
3. **Built-in X/Twitter access** - no separate Twitter API needed ($100-500/month saved)
4. **Multi-source search** - X, web, news all in one API
5. **Perfect for validation** - test if users want this feature before investing in complex RAG
6. **Similar to existing code** - matches your `GeminiClient` pattern
7. **Cost is predictable** - $0.025 per source with clear tracking

### Cost-Benefit Analysis

**Grok MVP Costs**:

- Development: 1 week (~40 hours)
- Monthly API: $750-1000 (100 queries/day × 10 sources)
- **Time to market: 1 week**

**RAG First Costs**:

- Development: 4 weeks (~160 hours)
- Monthly: $30-100 (cheaper per query)
- Requires Twitter API: +$100-500/month anyway
- **Time to market: 4 weeks**

**Winner**: Grok MVP provides 4x faster validation with built-in Twitter access

### Implementation Strategy

```
Week 1: Grok MVP
  ├─ Day 1-2: GrokClient implementation
  ├─ Day 3-4: NewsAnalysisService + API endpoint
  ├─ Day 5: Frontend integration
  └─ Day 6-7: Testing + cost monitoring

Week 2+: Monitor usage & gather feedback
  └─ Decision point after 2-4 weeks:
      ├─ High usage? → Add RAG for cost optimization
      ├─ Medium usage? → Keep Grok, add caching
      └─ Low usage? → Feature validated as not needed
```

### Success Metrics

- User engagement with news analysis feature
- Number of queries per day (target: 50-100)
- User feedback on relevance (target: 4+ stars)
- Response time (target: < 5 seconds)
- **Cost per query (target: < $0.30)**
- Citation quality (target: 80%+ relevant sources)

### Decision Point (After 2-4 weeks of Grok MVP)

**If HIGH engagement (100+ queries/day)**:

- → Proceed to Phase 2: Add RAG for cost optimization
- → Implement caching to reduce Grok API calls
- → Expected monthly cost reduction: 30-50%

**If MEDIUM engagement (20-50 queries/day)**:

- → Keep Grok only
- → Add basic caching
- → Monitor costs monthly

**If LOW engagement (< 20 queries/day)**:

- → Keep simple Grok implementation
- → Feature validated but not critical
- → Costs manageable without optimization

---

## Technical Debt Considerations

### Monitoring & Observability

- Add logging for all external API calls
- Track embedding generation costs
- Monitor vector search performance
- Alert on data ingestion failures

### Testing Strategy

- Unit tests for embedding generation
- Integration tests for vector search
- E2E tests for full analysis pipeline
- Load testing for concurrent queries

### Documentation

- API documentation for new endpoints
- Data source configuration guide
- Deployment guide for pgvector
- Troubleshooting guide for common issues

---

## Grok API Implementation Details

### Quick Start Guide

#### 1. Account Setup

1. Create xAI account at https://accounts.x.ai/sign-up
2. Load credits at https://console.x.ai/
3. Generate API key at https://console.x.ai/team/default/api-keys
4. Add to `application.yml`:

```yaml
xai:
  api:
    key: ${XAI_API_KEY:your-api-key-here}
```

#### 2. GrokClient Implementation

```java
package com.smu.tariff.ai;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GrokClient {

    private static final String GROK_API_URL = "https://api.x.ai/v1/chat/completions";
    private static final String MODEL_FAST = "grok-4-fast-reasoning"; // $0.20/$0.50
    private static final String MODEL_MINI = "grok-3-mini"; // $0.30/$0.50 - cheaper

    // Trade-related X handles to monitor
    private static final List<String> TRADE_HANDLES = List.of(
        "POTUS", "USTR", "tradegovuk", "EU_Commission",
        "wto", "IMFNews", "WorldBank", "BISCaribbean"
    );

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GrokClient(
            @Value("${xai.api.key:}") String apiKey,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(45))
                .build();
        this.objectMapper = objectMapper;
    }

    public GrokNewsAnalysis analyzeTradeNews(String query, int maxSources) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("xAI API key is not configured");
        }

        try {
            // Build search parameters for live search
            Map<String, Object> searchParams = buildSearchParameters(maxSources);

            // Build request
            Map<String, Object> requestBody = Map.of(
                "model", MODEL_FAST,
                "messages", List.of(
                    Map.of("role", "system", "content",
                        "You are a trade policy analyst. Analyze recent news and social media " +
                        "discussions about international trade, tariffs, and trade agreements. " +
                        "Provide insights on potential impacts on import/export costs."),
                    Map.of("role", "user", "content", query)
                ),
                "search_parameters", searchParams,
                "temperature", 0.7,
                "max_tokens", 2000
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                GROK_API_URL, entity, String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException(
                    "Grok API request failed with status " + response.getStatusCode()
                );
            }

            return parseGrokResponse(response.getBody());

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to analyze news with Grok", ex);
        }
    }

    private Map<String, Object> buildSearchParameters(int maxSources) {
        // Configure live search with X, web, and news sources
        return Map.of(
            "mode", "on", // Enable live search
            "return_citations", true, // Get source URLs
            "max_search_results", maxSources, // Limit sources for cost control
            "from_date", LocalDate.now().minusDays(7).toString(), // Last 7 days
            "sources", List.of(
                // X (Twitter) posts from trade-related accounts
                Map.of(
                    "type", "x",
                    "included_x_handles", TRADE_HANDLES,
                    "post_favorite_count", 50 // Only popular posts
                ),
                // Web search
                Map.of(
                    "type", "web",
                    "safe_search", true
                ),
                // News sources
                Map.of(
                    "type", "news",
                    "safe_search", true
                )
            )
        );
    }

    private GrokNewsAnalysis parseGrokResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // Extract analysis text
        String analysis = root.path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText();

        // Extract citations (source URLs)
        List<String> citations = objectMapper.convertValue(
            root.path("citations"),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );

        // Extract usage information for cost tracking
        JsonNode usage = root.path("usage");
        int promptTokens = usage.path("prompt_tokens").asInt();
        int completionTokens = usage.path("completion_tokens").asInt();
        int sourcesUsed = usage.path("num_sources_used").asInt();

        // Calculate cost
        double modelCost = (promptTokens * 0.20 / 1_000_000) +
                          (completionTokens * 0.50 / 1_000_000);
        double searchCost = sourcesUsed * 0.025; // $25 per 1000 sources
        double totalCost = modelCost + searchCost;

        return new GrokNewsAnalysis(
            analysis,
            citations,
            sourcesUsed,
            totalCost,
            promptTokens,
            completionTokens
        );
    }

    // DTO for response
    public record GrokNewsAnalysis(
        String analysis,
        List<String> citations,
        int sourcesUsed,
        double costUsd,
        int promptTokens,
        int completionTokens
    ) {}
}
```

#### 3. Service Layer

```java
package com.smu.tariff.news;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smu.tariff.ai.GrokClient;
import com.smu.tariff.ai.GrokClient.GrokNewsAnalysis;
import com.smu.tariff.user.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NewsAnalysisService {

    private static final int DEFAULT_MAX_SOURCES = 5; // Conservative for cost control
    private static final int MAX_QUERIES_PER_USER_PER_DAY = 10;
    private static final double DAILY_COST_CAP_USD = 50.0;

    private final GrokClient grokClient;
    private final NewsQueryRepository newsQueryRepository;
    private final CostTrackingService costTrackingService;

    public NewsAnalysisService(
            GrokClient grokClient,
            NewsQueryRepository newsQueryRepository,
            CostTrackingService costTrackingService) {
        this.grokClient = grokClient;
        this.newsQueryRepository = newsQueryRepository;
        this.costTrackingService = costTrackingService;
    }

    @Transactional
    public NewsAnalysisResponse analyzeNews(String query, User user) {
        // Rate limiting
        checkUserRateLimit(user);
        checkDailyCostCap();

        // Execute analysis with Grok
        GrokNewsAnalysis grokResult = grokClient.analyzeTradeNews(query, DEFAULT_MAX_SOURCES);

        // Log query for tracking
        NewsQuery newsQuery = new NewsQuery(
            user,
            query,
            grokResult.analysis(),
            grokResult.citations(),
            grokResult.sourcesUsed(),
            grokResult.costUsd(),
            LocalDateTime.now()
        );
        newsQueryRepository.save(newsQuery);

        // Track costs
        costTrackingService.recordCost(grokResult.costUsd());

        return new NewsAnalysisResponse(
            grokResult.analysis(),
            grokResult.citations(),
            grokResult.sourcesUsed(),
            grokResult.costUsd()
        );
    }

    private void checkUserRateLimit(User user) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayQueries = newsQueryRepository.countByUserAndCreatedAtAfter(user, today);

        if (todayQueries >= MAX_QUERIES_PER_USER_PER_DAY) {
            throw new RateLimitException(
                "Daily query limit reached. You can make " +
                MAX_QUERIES_PER_USER_PER_DAY + " queries per day."
            );
        }
    }

    private void checkDailyCostCap() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        double todayCosts = costTrackingService.getTotalCostsSince(today);

        if (todayCosts >= DAILY_COST_CAP_USD) {
            throw new CostCapException(
                "Daily cost cap reached. Please try again tomorrow."
            );
        }
    }
}
```

#### 4. Controller

```java
package com.smu.tariff.news;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.smu.tariff.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/news")
public class NewsAnalysisController {

    private final NewsAnalysisService newsAnalysisService;

    public NewsAnalysisController(NewsAnalysisService newsAnalysisService) {
        this.newsAnalysisService = newsAnalysisService;
    }

    @PostMapping("/analyze")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NewsAnalysisResponse> analyzeNews(
            @Valid @RequestBody NewsAnalysisRequest request,
            @AuthenticationPrincipal User user) {

        NewsAnalysisResponse response = newsAnalysisService.analyzeNews(
            request.query(),
            user
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NewsQueryDto>> getQueryHistory(
            @AuthenticationPrincipal User user) {
        // Return user's past queries
        // Implementation similar to QueryLogController
        return ResponseEntity.ok(List.of());
    }

    // DTOs
    public record NewsAnalysisRequest(
        @NotBlank(message = "Query cannot be empty")
        @Size(min = 10, max = 500, message = "Query must be between 10 and 500 characters")
        String query
    ) {}
}
```

#### 5. Database Schema

```java
@Entity
@Table(name = "news_queries")
public class NewsQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    @Column(columnDefinition = "TEXT[]")
    private String[] citationUrls;

    @Column(name = "sources_used")
    private int sourcesUsed;

    @Column(name = "cost_usd", precision = 10, scale = 4)
    private double costUsd;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors, getters, setters
}
```

### Cost Control Best Practices

1. **Start with 5 sources max** (5 × $0.025 = $0.125 per query)
2. **Implement caching** for identical queries within 1-6 hours
3. **User rate limits**: 10 queries per user per day
4. **Daily cost cap**: $50/day system-wide
5. **Use Grok-3-mini** for simple queries (cheaper)
6. **Monitor usage dashboard**: Track costs in real-time

### Expected Costs (Conservative Estimates)

**Scenario 1: Light Usage (20 queries/day)**

- 20 queries × 5 sources × $0.025 = $2.50/day
- Monthly: ~$75

**Scenario 2: Medium Usage (100 queries/day)**

- 100 queries × 5 sources × $0.025 = $12.50/day
- Monthly: ~$375

**Scenario 3: Heavy Usage (500 queries/day)**

- 500 queries × 5 sources × $0.025 = $62.50/day
- Monthly: ~$1,875

**Recommendation**: Start with Scenario 1 or 2, add caching to reduce costs

---

## Conclusion (UPDATED)

The **Grok API being publicly available changes everything**. The original recommendation of starting with RAG is no longer optimal.

### New Recommendation: Start with Grok AI MVP ⭐

**Key Advantages**:

1. ✅ **Available NOW** - no waiting, no uncertainty
2. ✅ **1 week implementation** - 4x faster than RAG
3. ✅ **Built-in X/Twitter** - saves $100-500/month on separate Twitter API
4. ✅ **Multi-source search** - X, web, news all included
5. ✅ **OpenAI compatible** - familiar SDK, easy integration
6. ✅ **Perfect for validation** - test feature demand quickly

**Trade-offs**:

- ⚠️ Higher per-query cost (~$0.15-0.30 vs RAG's ~$0.01)
- ⚠️ Cost scales with usage (need rate limiting)
- ⚠️ Live Search API deprecating Dec 2025 (migration path exists)

**When to Add RAG**:

- After Grok MVP proves valuable (2-4 weeks)
- When query volume exceeds 100/day (cost optimization)
- When users need deep historical analysis (3+ months)

### Implementation Timeline

```
Week 1: Grok MVP
├─ ✅ Fastest path to production
├─ ✅ Real-time X/Twitter insights
└─ ✅ Validate feature demand

Weeks 2-4: Monitor & Optimize
├─ Track usage patterns
├─ Gather user feedback
└─ Decide on RAG addition

Month 2+: Scale (if needed)
└─ Add RAG for cost optimization
```

### Bottom Line

While RAG provides better long-term cost efficiency, **Grok offers superior time-to-market** with built-in real-time data access. For a feature that's unproven in your app, Grok's 1-week implementation provides the fastest validation path.

**Start with Grok, add RAG later if needed** - this minimizes risk and maximizes learning speed.

Would you like me to proceed with creating the implementation code for the Grok MVP?
