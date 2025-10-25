# News Sidebar Troubleshooting Guide

## Issue: Sidebar Not Showing Up

If you don't see the news sidebar on the Calculate page, follow these steps:

### Step 1: Check Backend is Running

1. Open a terminal in the `backend` folder
2. Run: `mvnw spring-boot:run` (or `./mvnw spring-boot:run` on Mac/Linux)
3. Wait for the message: "Started TariffApplication"
4. Verify it's running on port 8080

### Step 2: Check Frontend is Running

1. Open a terminal in the `frontend` folder
2. Run: `npm run dev` (or `pnpm dev`)
3. Note the URL (usually http://localhost:5173)
4. Open the URL in your browser

### Step 3: Check Authentication

The news endpoint requires authentication. You must be logged in!

1. Go to the Login page: http://localhost:5173/login
2. Log in with your credentials
3. Then navigate to the Calculate page

**Important**: If you're not logged in, the API will return 401 Unauthorized and the news won't load.

### Step 4: Check Browser Console

1. Open the Calculate page
2. Press F12 to open Developer Tools
3. Go to the "Console" tab
4. Look for errors (red text)

**Common errors:**

- `401 Unauthorized` → You're not logged in. Go to /login first.
- `Network Error` → Backend is not running. Start the backend server.
- `Failed to fetch news` → Check the error details in console.

### Step 5: Check Network Tab

1. Open Developer Tools (F12)
2. Go to "Network" tab
3. Refresh the Calculate page
4. Look for a request to `/api/news/tariff`

**What to check:**

- **Status Code**: Should be 200 (success)
  - 401 = Not authenticated
  - 404 = Endpoint not found
  - 500 = Server error
- **Response**: Click on the request, go to "Response" tab
  - Should see JSON with `articles` array
- **Headers**: Check "Request Headers"
  - Should have `Authorization: Bearer <token>`
  - If missing, you're not logged in

### Step 6: Verify API Key

1. Open `backend/src/main/resources/application.yml`
2. Check the `newsdata.api.key` value
3. It should NOT be "YOUR_NEWSDATA_API_KEY"
4. Get a real API key from https://newsdata.io/register

**Current key in file:**

```yaml
newsdata:
  api:
    key: pub_7713507262314bd18c0fb64c8536c7bc
```

This key looks valid! ✅

### Step 7: Check Sidebar Visibility

The sidebar might be collapsed or hidden due to screen size.

1. On the Calculate page, look for "📰 Latest Tariff News" on the right side
2. If you see it but it's collapsed, click on it to expand
3. If you don't see it at all, check the page width
   - **Desktop (>1280px)**: Sidebar on right, 380px wide
   - **Laptop (1024-1280px)**: Sidebar on right, 320px wide
   - **Tablet (<1024px)**: Sidebar below calculator
   - **Mobile (<768px)**: Sidebar below calculator

Try zooming out or making your browser window wider.

## Manual Testing

### Test 1: Direct API Call (with curl)

**Windows (PowerShell):**

```powershell
# First, login to get a token
$body = @{
    username = "your_username"
    password = "your_password"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
$token = $response.token

# Then fetch news
$headers = @{
    Authorization = "Bearer $token"
}
Invoke-RestMethod -Uri "http://localhost:8080/api/news/tariff?limit=5" -Headers $headers
```

**Mac/Linux (curl):**

```bash
# First, login to get a token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}' \
  | jq -r '.token')

# Then fetch news
curl http://localhost:8080/api/news/tariff?limit=5 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**

```json
{
  "status": "success",
  "totalResults": 42,
  "articles": [
    {
      "articleId": "...",
      "title": "New Tariffs...",
      "description": "...",
      "link": "https://...",
      "pubDate": "2025-10-24 12:00:00",
      "sourceId": "reuters",
      "imageUrl": "https://..."
    }
  ]
}
```

### Test 2: Check Endpoint in Swagger

1. Go to: http://localhost:8080/swagger-ui/index.html
2. Find "News" section
3. Click on `GET /api/news/tariff`
4. Click "Try it out"
5. Click "Authorize" (top right)
6. Login first, then copy the JWT token
7. Paste token in the "Authorize" dialog
8. Click "Execute"

**Expected**: Response with status 200 and articles array

## Common Issues & Solutions

### Issue: "Failed to load news" Error

**Cause**: API request failed

**Solutions**:

1. Check browser console for exact error
2. Verify backend is running
3. Check you're logged in
4. Verify API key is set

### Issue: Sidebar is Empty (No Articles)

**Cause**: NewsData.io returned no results

**Solutions**:

1. Try selecting "All Countries" in the filter
2. Check if your API key has remaining quota
3. NewsData.io free tier: 200 requests/day
4. Check API usage at: https://newsdata.io/dashboard

### Issue: "401 Unauthorized" Error

**Cause**: Not logged in or token expired

**Solutions**:

1. Go to `/login` and log in
2. Token expires after 24 hours
3. Log out and log back in
4. Check localStorage for `token` key (F12 → Application → Local Storage)

### Issue: Sidebar Appears But No Content Loads

**Cause**: Loading state stuck

**Solutions**:

1. Check browser console for errors
2. Click "Refresh News" button in sidebar
3. Hard refresh page (Ctrl+Shift+R or Cmd+Shift+R)
4. Clear browser cache

### Issue: Images Not Loading in Articles

**Cause**: Image URLs are broken or blocked

**Solutions**:

- This is normal - some news sources don't provide images
- Articles without images will hide the image section automatically
- No action needed

## Verification Checklist

Use this checklist to verify everything is working:

- [ ] Backend server is running (port 8080)
- [ ] Frontend dev server is running (port 5173)
- [ ] You are logged in (check localStorage for token)
- [ ] NewsData.io API key is set in application.yml
- [ ] You can see the Calculate page
- [ ] You can see "📰 Latest Tariff News" heading on the right
- [ ] Country filter dropdown is visible and works
- [ ] Articles are loading (or you see a loading spinner)
- [ ] No red errors in browser console
- [ ] Network tab shows 200 response for /api/news/tariff

## Still Not Working?

### Enable Debug Logging

Add this to `application.yml`:

```yaml
logging:
  level:
    com.smu.tariff.news: DEBUG
    org.springframework.web: DEBUG
```

Restart backend and check console logs for news API requests.

### Check Frontend Environment

Verify `frontend/.env` or `frontend/.env.local`:

```env
VITE_API_URL=http://localhost:8080
```

If missing, create this file.

### Reset Everything

1. Stop backend and frontend
2. Clear browser cache and localStorage
3. Delete `backend/target` folder
4. Restart backend: `mvnw spring-boot:run`
5. Restart frontend: `npm run dev`
6. Log in fresh
7. Navigate to Calculate page

## Success Indicators

You'll know it's working when you see:

1. ✅ Sidebar visible on right side of Calculate page
2. ✅ Country filter dropdown with all countries
3. ✅ News articles with titles, descriptions, sources
4. ✅ Timestamps like "2h ago", "Yesterday"
5. ✅ Click on article opens in new tab
6. ✅ Refresh button updates articles
7. ✅ Console shows: "GET /api/news/tariff 200 OK"

## Need More Help?

Check these files for implementation details:

- Frontend: `frontend/src/components/TariffNewsSidebar.jsx`
- Backend: `backend/src/main/java/com/smu/tariff/news/NewsController.java`
- Config: `backend/src/main/resources/application.yml`
- Docs: `docs/NEWS-SIDEBAR-FEATURE.md`
