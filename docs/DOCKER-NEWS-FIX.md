# Docker Configuration Fix - News Sidebar

## What Was Fixed

### Problem

The news sidebar wasn't showing in Docker because:

1. Frontend was using `http://localhost:8080` which doesn't work inside Docker containers
2. Nginx proxy path was incorrect (`/api/` → `/` instead of `/api/` → `/api/`)
3. Environment variables for API keys weren't passed to backend container

### Solution Applied

#### 1. **Frontend API Configuration** (`frontend/src/services/api.js`)

Changed from:

```javascript
const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";
```

To:

```javascript
// In production (Docker), use relative URLs - nginx handles proxying
// In development, use localhost:8080 directly
const API_BASE =
  import.meta.env.VITE_API_URL ||
  (import.meta.env.PROD ? "" : "http://localhost:8080");
```

This means:

- **Development** (`npm run dev`): Uses `http://localhost:8080` directly
- **Docker** (production build): Uses relative URLs like `/api/news/tariff`, nginx proxies to backend

#### 2. **Nginx Proxy Configuration** (`frontend/nginx.conf`)

Fixed the proxy path from:

```nginx
location /api/ {
    proxy_pass http://backend:8080/;  # ❌ WRONG - strips /api/
}
```

To:

```nginx
location /api/ {
    proxy_pass http://backend:8080/api/;  # ✅ CORRECT - preserves /api/
}
```

Now requests to `http://localhost/api/news/tariff` → `http://backend:8080/api/news/tariff`

#### 3. **Docker Environment Variables** (`docker-compose.dev.yml`)

Added environment variables for API keys:

```yaml
environment:
  GEMINI_API_KEY: ${GEMINI_API_KEY:-AIzaSyCxgqgrYbMCQs9178XiPkabKLKkMGi9q-g}
  NEWSDATA_API_KEY: ${NEWSDATA_API_KEY:-pub_7713507262314bd18c0fb64c8536c7bc}
```

#### 4. **Backend Configuration** (`backend/src/main/resources/application.yml`)

Updated to use environment variables with fallback defaults:

```yaml
gemini:
  api:
    key: ${GEMINI_API_KEY:AIzaSyCxgqgrYbMCQs9178XiPkabKLKkMGi9q-g}

newsdata:
  api:
    key: ${NEWSDATA_API_KEY:pub_7713507262314bd18c0fb64c8536c7bc}
```

## How to Apply the Fix

### Step 1: Create .env File (if not exists)

```bash
# Copy the example file
copy .env.example .env
```

The .env file should already have the correct values.

### Step 2: Rebuild Docker Containers

**Option A - Use the Script (Recommended):**

```bash
docker-rebuild.bat
```

**Option B - Manual Commands:**

```bash
# Stop containers
docker-compose -f docker-compose.dev.yml down

# Rebuild (no cache to ensure changes take effect)
docker-compose -f docker-compose.dev.yml build --no-cache

# Start containers
docker-compose -f docker-compose.dev.yml up -d
```

### Step 3: Verify It's Working

1. **Check containers are running:**

   ```bash
   docker-compose -f docker-compose.dev.yml ps
   ```

   All should show "Up" status.

2. **Check backend logs:**

   ```bash
   docker-compose -f docker-compose.dev.yml logs backend
   ```

   Look for:

   - `Started TariffApplication`
   - No errors about missing API keys

3. **Check frontend logs:**

   ```bash
   docker-compose -f docker-compose.dev.yml logs frontend
   ```

4. **Open the app:**

   - Go to: http://localhost:80
   - Log in
   - Navigate to Calculate page
   - Look for "📰 Latest Tariff News" sidebar on the right

5. **Check browser console (F12):**
   - Should see: `GET /api/news/tariff 200 OK`
   - NOT: `404` or `Network Error`

## Network Flow in Docker

```
Browser (http://localhost:80)
    ↓
Nginx Container (frontend:80)
    ↓ (for /api/* requests)
Backend Container (backend:8080)
    ↓
NewsData.io API
```

**Example Request Flow:**

1. Browser: `GET http://localhost:80/api/news/tariff?limit=8`
2. Nginx receives request at `/api/news/tariff`
3. Nginx proxies to: `http://backend:8080/api/news/tariff`
4. Backend fetches from NewsData.io
5. Backend returns JSON to Nginx
6. Nginx returns to browser

## Troubleshooting

### News Still Not Showing

**Check 1: Are containers running?**

```bash
docker-compose -f docker-compose.dev.yml ps
```

**Check 2: Can you reach the backend?**

```bash
# Should return HTML (Swagger UI)
curl http://localhost:8080
```

**Check 3: Is the proxy working?**

```bash
# Should return same as above (proxied through nginx)
curl http://localhost:80/api
```

**Check 4: Check nginx logs**

```bash
docker-compose -f docker-compose.dev.yml logs frontend | findstr "error"
```

**Check 5: Check backend logs**

```bash
docker-compose -f docker-compose.dev.yml logs backend | findstr "error"
```

### Backend Returns 404 for /api/news/tariff

**Issue**: NewsController might not be loaded

**Fix**:

1. Check if NewsController.java exists
2. Rebuild backend: `docker-compose -f docker-compose.dev.yml build backend`
3. Restart: `docker-compose -f docker-compose.dev.yml restart backend`

### CORS Errors in Browser Console

**Issue**: Cross-Origin Resource Sharing blocked

**Fix**: Already applied in nginx.conf:

```nginx
add_header Access-Control-Allow-Origin * always;
```

If still seeing errors, try:

```bash
docker-compose -f docker-compose.dev.yml restart frontend
```

## Development vs Docker Comparison

| Aspect       | Development (npm run dev) | Docker (docker-compose)        |
| ------------ | ------------------------- | ------------------------------ |
| Frontend URL | http://localhost:5173     | http://localhost:80            |
| Backend URL  | http://localhost:8080     | http://backend:8080 (internal) |
| API Calls    | Direct to localhost:8080  | Through nginx proxy            |
| Hot Reload   | ✅ Yes                    | ❌ No (need rebuild)           |
| Environment  | .env.local                | .env                           |
| Best For     | Coding/debugging          | Testing production-like setup  |

## Common Commands

```bash
# View all logs
docker-compose -f docker-compose.dev.yml logs -f

# View specific service logs
docker-compose -f docker-compose.dev.yml logs -f frontend
docker-compose -f docker-compose.dev.yml logs -f backend

# Restart a service
docker-compose -f docker-compose.dev.yml restart frontend

# Stop all
docker-compose -f docker-compose.dev.yml down

# Remove volumes (fresh database)
docker-compose -f docker-compose.dev.yml down -v

# Execute command in container
docker-compose -f docker-compose.dev.yml exec backend sh
docker-compose -f docker-compose.dev.yml exec frontend sh
```

## Files Changed

1. ✅ `frontend/src/services/api.js` - Use relative URLs in production
2. ✅ `frontend/nginx.conf` - Fix proxy path to preserve /api/
3. ✅ `docker-compose.dev.yml` - Add API key environment variables
4. ✅ `backend/src/main/resources/application.yml` - Use env vars with defaults
5. ✅ `.env.example` - Added NEWSDATA_API_KEY

## Next Steps

After rebuilding, the news sidebar should appear at http://localhost:80/calculate (after logging in).

If issues persist, check the troubleshooting guide in `docs/NEWS-SIDEBAR-TROUBLESHOOTING.md`.
