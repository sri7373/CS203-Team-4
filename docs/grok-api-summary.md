# Grok API - Key Information Summary

## 🎉 Major Update: Grok API is Now Available!

The xAI Grok API is **publicly available** as of October 2025, which significantly changes the feasibility analysis for adding news analysis to your TariffSheriff application.

---

## Quick Facts

| Aspect            | Details                                                  |
| ----------------- | -------------------------------------------------------- |
| **API Status**    | ✅ Publicly available NOW                                |
| **Access**        | https://console.x.ai/ - Create account, generate API key |
| **Compatibility** | OpenAI SDK compatible (drop-in replacement)              |
| **Endpoint**      | `https://api.x.ai/v1/chat/completions`                   |
| **Documentation** | https://docs.x.ai/                                       |

---

## Key Features

### 1. Live Search (Real-time Data Access)

- **Built-in X/Twitter access** - No separate Twitter API needed! 🎯
- **Multi-source search**: X posts, web, news, RSS feeds
- **Date range filtering**: Query specific time periods
- **X handle filtering**: Focus on specific accounts (e.g., @POTUS, @USTR, @WTO)
- **Engagement filtering**: Filter by post likes, views
- **Citation support**: Returns source URLs automatically

### 2. Cost Structure

#### Model Pricing (per 1M tokens)

| Model                     | Input | Output | Best For                         |
| ------------------------- | ----- | ------ | -------------------------------- |
| **grok-4-fast-reasoning** | $0.20 | $0.50  | ⭐ Recommended for your use case |
| grok-4-fast-non-reasoning | $0.20 | $0.50  | Faster responses                 |
| grok-code-fast-1          | $0.20 | $1.50  | Coding tasks                     |
| grok-4                    | $3.00 | $15.00 | Most intelligent (expensive)     |
| grok-3-mini               | $0.30 | $0.50  | Budget option                    |

#### Live Search Pricing

- **$25 per 1,000 sources** = **$0.025 per source**
- "Source" = each article/post/webpage retrieved
- Cost tracked in response: `usage.num_sources_used`

#### Example Query Cost

```
Query: "How will US-China trade tensions affect semiconductor tariffs?"

Model costs:
- Input (500 tokens): $0.0001
- Output (1,000 tokens): $0.0005

Live Search costs:
- 5 sources retrieved: 5 × $0.025 = $0.125

Total: ~$0.13 per query
```

### 3. Monthly Cost Estimates

**Conservative (5 sources per query)**:

- 20 queries/day: ~$75/month
- 100 queries/day: ~$375/month
- 500 queries/day: ~$1,875/month

**Cost Control Strategies**:

- Start with 5 sources max (vs default 20)
- Implement per-user rate limits (10 queries/day)
- Cache identical queries for 1-6 hours
- Use daily cost caps ($50/day)

---

## Integration with Your App

### Why It's Perfect for TariffSheriff

1. **✅ Similar to Existing Code**: Your `GeminiClient` pattern works perfectly
2. **✅ OpenAI Compatible**: Can use OpenAI SDK with URL change
3. **✅ Trade-Focused Data**: Monitor trade policy X accounts (@USTR, @WTO, etc.)
4. **✅ Real-time Insights**: Breaking news on tariff changes
5. **✅ Source Attribution**: Citations for credibility
6. **✅ Fast Implementation**: ~1 week vs 4 weeks for RAG

### Implementation Comparison

| Approach     | Time    | Cost/Month | X Access                        | Complexity |
| ------------ | ------- | ---------- | ------------------------------- | ---------- |
| **Grok API** | 1 week  | $75-1000   | ✅ Built-in                     | Low        |
| RAG System   | 4 weeks | $30-100    | ❌ Need Twitter API (+$100-500) | High       |
| Hybrid       | 6 weeks | $800-2500  | ✅ Built-in                     | Medium     |

---

## Sample Integration Code

### Simple Example (cURL)

```bash
curl https://api.x.ai/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -d '{
    "model": "grok-4-fast-reasoning",
    "messages": [
      {
        "role": "user",
        "content": "Analyze recent US-China trade news"
      }
    ],
    "search_parameters": {
      "mode": "on",
      "max_search_results": 5,
      "return_citations": true,
      "sources": [
        {
          "type": "x",
          "included_x_handles": ["USTR", "POTUS", "WTO"],
          "post_favorite_count": 50
        },
        {"type": "web"},
        {"type": "news"}
      ]
    }
  }'
```

### Java Integration (Spring Boot)

```java
@Component
public class GrokClient {
    private static final String GROK_API_URL = "https://api.x.ai/v1/chat/completions";
    private final RestTemplate restTemplate;

    public GrokNewsAnalysis analyzeTradeNews(String query, int maxSources) {
        // Build request with search parameters
        Map<String, Object> request = Map.of(
            "model", "grok-4-fast-reasoning",
            "messages", List.of(Map.of("role", "user", "content", query)),
            "search_parameters", buildSearchParams(maxSources)
        );

        // Make API call (similar to GeminiClient)
        ResponseEntity<String> response = restTemplate.postForEntity(
            GROK_API_URL,
            new HttpEntity<>(request, headers),
            String.class
        );

        // Parse response with citations and cost tracking
        return parseResponse(response.getBody());
    }
}
```

---

## Trade-Related X Handles to Monitor

```java
private static final List<String> TRADE_HANDLES = List.of(
    "POTUS",           // US President
    "USTR",            // US Trade Representative
    "tradegovuk",      // UK Department for Trade
    "EU_Commission",   // European Commission
    "wto",             // World Trade Organization
    "IMFNews",         // International Monetary Fund
    "WorldBank",       // World Bank
    "BISCaribbean",    // Bank for International Settlements
    "trade",           // General trade discussions
    "CustomsBorder"    // US Customs and Border Protection
);
```

---

## Important Notes

### ⚠️ Live Search API Deprecation

- Current Live Search API will be **deprecated December 15, 2025**
- Migration path: New [Agentic Tool Calling API](https://docs.x.ai/docs/guides/tools/overview)
- Your implementation will need updating in ~1 year
- xAI will provide migration guide

### ✅ Risk Mitigation

- **Start small**: 5 sources per query, 10 queries per user per day
- **Monitor costs**: Log every query cost in database
- **Set caps**: Daily system-wide cost limit ($50/day recommended)
- **Cache aggressively**: Identical queries within 6 hours reuse results
- **User feedback**: Track if users find insights valuable

---

## Recommendation: Grok MVP First ⭐

### Why Start with Grok (Not RAG)

1. ✅ **4x faster to market**: 1 week vs 4 weeks
2. ✅ **No Twitter API needed**: Saves $100-500/month
3. ✅ **Validates demand quickly**: Know if users want this feature
4. ✅ **Real-time data**: Breaking news and trending topics
5. ✅ **Can add RAG later**: For cost optimization if usage is high

### When to Add RAG

- ✅ After 2-4 weeks of Grok MVP usage
- ✅ If queries exceed 100/day (cost optimization needed)
- ✅ If users need deep historical analysis (3+ months back)
- ✅ If you want to reduce per-query costs by 70-90%

### Hybrid Approach (Long-term)

```
User Query → Check if historical (>30 days)
    ├─→ YES: Use RAG (cheap, cached)
    └─→ NO: Use Grok (real-time, expensive)
```

---

## Next Steps

1. **Create xAI account**: https://console.x.ai/
2. **Generate API key**: Add to `application.yml`
3. **Implement GrokClient**: ~4-8 hours (see full code in feasibility doc)
4. **Create NewsAnalysisService**: Rate limiting + cost tracking
5. **Build API endpoint**: `POST /api/news/analyze`
6. **Frontend integration**: News analysis page
7. **Deploy & monitor**: Track usage and costs

**Total implementation time: 1 week** 🚀

---

## Resources

- **API Console**: https://console.x.ai/
- **Documentation**: https://docs.x.ai/
- **API Reference**: https://docs.x.ai/docs/api-reference
- **Live Search Guide**: https://docs.x.ai/docs/guides/live-search
- **Pricing**: https://x.ai/api#pricing
- **Status Page**: https://status.x.ai/

---

## Questions?

See the full feasibility analysis in `docs/news-analysis-feasibility.md` for:

- Detailed cost breakdowns
- Complete implementation code
- RAG comparison
- Hybrid architecture
- Security considerations
- Testing strategies
