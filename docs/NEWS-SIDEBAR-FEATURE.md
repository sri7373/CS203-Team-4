# Tariff News Sidebar Feature

## Overview

The Tariff News Sidebar displays real-time tariff-related news articles on the Calculate page, helping users stay informed about trade policy changes that might affect their tariff calculations.

## Implementation Details

### Components Created

#### 1. **TariffNewsSidebar.jsx** (`frontend/src/components/`)

A collapsible sidebar component that fetches and displays tariff news from the backend NewsData.io integration.

**Features:**

- Auto-fetches news based on destination country
- Collapsible sidebar to save screen space
- Loading states with spinner animation
- Error handling with retry functionality
- Article cards with:
  - Image thumbnails (when available)
  - Title (2-line clamp)
  - Description (2-line clamp, 100 char truncation)
  - Source and relative timestamp
  - Sentiment badges (positive/negative/neutral)
- Click to open article in new tab
- Refresh button to reload news
- Sticky positioning for scroll persistence

**Props:**

- `country` (string, optional): Country code to filter news (e.g., "US", "CN")
- `limit` (number, default: 5): Number of articles to display

**Styling:**

- Matches existing glass morphism design system
- Glow borders and neon text effects
- Framer Motion animations with stagger effect
- Hover effects on article cards
- Responsive max-height with scrollable content

### Modified Files

#### 2. **CalculatePage.jsx** (`frontend/src/pages/`)

Updated the Calculate page layout to accommodate the news sidebar.

**Changes:**

- Imported `TariffNewsSidebar` component
- Changed layout from single column to two-column grid:
  - Left column: Existing tariff calculator (responsive width)
  - Right column: News sidebar (380px fixed width)
- Passes `destination` country code to sidebar
- Responsive breakpoints:
  - Desktop (>1280px): 2 columns with 380px sidebar
  - Laptop (1024px-1280px): 2 columns with 320px sidebar
  - Tablet (<1024px): Single column, stacked layout
  - Mobile (<768px): Single column with shorter sidebar

#### 3. **styles.css** (`frontend/src/`)

Added responsive styles and utility classes for the news feature.

**Additions:**

- `.calculate-layout` responsive grid breakpoints
- `.tariff-news-sidebar` positioning adjustments
- `.spinner-small` for news loading animation
- `.btn-small` for action buttons in news cards
- `.small` and `.tiny` text size utilities

## API Integration

### Backend Endpoint Used

```
GET /api/news/tariff?country={code}&limit={num}
```

**Query Parameters:**

- `country` (optional): ISO country code (e.g., "US", "CN", "SG")
- `limit` (optional, default: 10): Number of articles to return

**Response Format:**

```json
{
  "status": "success",
  "totalResults": 42,
  "articles": [
    {
      "articleId": "unique-id",
      "title": "New Tariffs Announced...",
      "description": "Article description...",
      "link": "https://...",
      "pubDate": "2025-01-15 10:30:00",
      "sourceId": "reuters",
      "imageUrl": "https://...",
      "sentiment": "negative",
      "category": ["business", "politics"]
    }
  ],
  "nextPage": "token-for-pagination"
}
```

## User Experience

### Loading States

1. **Initial Load**: Shows spinner with "Loading news..." message
2. **Error State**: Displays error message with retry button
3. **Empty State**: Shows "No tariff news available" with refresh button
4. **Success State**: Displays article cards with smooth animations

### Interactions

- **Collapse/Expand**: Click header or arrow button to toggle sidebar
- **Read Article**: Click anywhere on article card to open in new tab
- **Refresh**: Click "Refresh News" button to reload articles
- **Country Filter**: Automatically updates when destination country changes

### Responsive Behavior

- **Desktop (>1280px)**: Full-width sidebar with 8 articles
- **Laptop (1024-1280px)**: Narrower sidebar with 8 articles
- **Tablet (<1024px)**: Full-width sidebar below calculator
- **Mobile (<768px)**: Shorter sidebar (500px max-height)

## Configuration

### Backend Setup Required

1. Add NewsData.io API key to `backend/src/main/resources/application.yml`:

   ```yaml
   newsdata:
     api:
       key: YOUR_NEWSDATA_API_KEY
   ```

2. Get API key from: https://newsdata.io/register
   - Free tier: 200 requests/day
   - Paid tiers: 350-10,000 requests/day

### Frontend Configuration

No additional configuration needed. The sidebar uses the existing:

- `api.js` for HTTP requests with JWT authentication
- CSS variables from `styles.css` for theming
- Framer Motion for animations

## Performance Considerations

### Caching

- News is fetched on component mount
- Re-fetches when destination country changes
- Manual refresh button for user-triggered updates
- Consider adding client-side caching (localStorage) for frequently accessed countries

### API Rate Limits

- Free tier: 200 requests/day
- Each page load = 1 request per country
- Sidebar refresh = 1 additional request
- Estimate: ~50-100 users/day on free tier

### Optimization Opportunities

1. **Cache responses** in localStorage for 15-30 minutes
2. **Debounce country changes** to avoid rapid API calls
3. **Lazy load images** in article cards
4. **Paginate articles** if showing more than 10
5. **Add service worker** for offline news display

## Testing Checklist

### Frontend Tests

- [ ] Sidebar loads on CalculatePage mount
- [ ] News fetches when country changes
- [ ] Collapse/expand animation works smoothly
- [ ] Article cards render correctly with all fields
- [ ] Missing image URLs handled gracefully
- [ ] Error state displays with retry button
- [ ] Empty state shows when no articles
- [ ] Responsive layout works on all screen sizes
- [ ] Hover effects work on article cards
- [ ] External links open in new tab

### Backend Tests

- [ ] `/api/news/tariff` endpoint returns 200 OK
- [ ] Country filter works correctly
- [ ] Limit parameter restricts article count
- [ ] Authentication required for endpoint
- [ ] NewsData.io API key is valid
- [ ] Error handling for API failures
- [ ] Rate limit handling (429 status)

### Integration Tests

- [ ] Calculate page with sidebar loads without errors
- [ ] News sidebar updates when selecting different countries
- [ ] JWT token is sent with news API requests
- [ ] News displays after successful tariff calculation
- [ ] Multiple users can fetch news concurrently

## Future Enhancements

### Phase 2 Features

1. **Search and Filter**

   - Search bar for keyword filtering
   - Category filters (business, politics, economy)
   - Date range filters

2. **Bookmarks**

   - Save favorite articles
   - View saved articles later
   - Export bookmarks as PDF

3. **Notifications**

   - Alert users to breaking tariff news
   - Email digests for selected countries
   - Push notifications for major changes

4. **Analytics**

   - Track most-viewed articles
   - Popular news topics
   - User engagement metrics

5. **AI Insights**
   - Summarize multiple articles
   - Extract tariff rate changes from news
   - Predict impact on user's trade routes

### Technical Debt

1. Add unit tests for TariffNewsSidebar component
2. Add E2E tests for Calculate page with sidebar
3. Implement client-side caching strategy
4. Add loading skeleton instead of spinner
5. Optimize bundle size (code splitting)

## Troubleshooting

### Common Issues

**Sidebar shows "Failed to load news"**

- Check backend is running on port 8080
- Verify NewsData.io API key is set in application.yml
- Check browser console for detailed error messages
- Verify user is authenticated (JWT token valid)

**News doesn't update when country changes**

- Check React DevTools to see if `destination` prop is updating
- Verify useEffect dependency array includes `country`
- Check network tab for API requests

**Articles don't open when clicked**

- Verify article has valid `link` property
- Check browser popup blocker settings
- Inspect article data in console

**Sidebar layout breaks on mobile**

- Check responsive media queries in styles.css
- Verify grid layout switches to single column
- Test on actual device, not just browser dev tools

## Related Documentation

- [Backend NewsData.io Integration](./NEWSDATA-INTEGRATION-GUIDE.md)
- [API Route Documentation](./API_ROUTE_DOCUMENTATION.md)
- [Calculate Page Architecture](./backend-architecture.md)

## Changelog

### v1.0.0 (2025-01-15)

- Initial release of Tariff News Sidebar feature
- Integration with CalculatePage
- Responsive design for all screen sizes
- Error handling and loading states
- Sentiment analysis badges
