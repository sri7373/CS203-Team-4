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
