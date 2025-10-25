@echo off
REM Rebuild only the frontend container
echo ========================================
echo Rebuilding Frontend Container
echo ========================================
echo.

echo Stopping frontend container...
docker-compose -f docker-compose.dev.yml stop frontend

echo.
echo Removing frontend container...
docker-compose -f docker-compose.dev.yml rm -f frontend

echo.
echo Rebuilding frontend (no cache)...
docker-compose -f docker-compose.dev.yml build --no-cache frontend

echo.
echo Starting frontend container...
docker-compose -f docker-compose.dev.yml up -d frontend

echo.
echo ========================================
echo Frontend rebuilt and started!
echo ========================================
echo.
echo Frontend: http://localhost:80
echo.
echo Checking container status...
docker-compose -f docker-compose.dev.yml ps frontend

echo.
echo To view frontend logs:
echo   docker-compose -f docker-compose.dev.yml logs -f frontend
echo.
![1761413290676](image/docker-rebuild-frontend/1761413290676.png)![1761413302182](image/docker-rebuild-frontend/1761413302182.png)![1761415988107](image/docker-rebuild-frontend/1761415988107.png)![1761416004479](image/docker-rebuild-frontend/1761416004479.png)