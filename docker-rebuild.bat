@echo off
REM Docker Development Environment - Rebuild and Restart Script
REM This script stops, rebuilds, and restarts all Docker containers

echo ========================================
echo Tariff Calculator - Docker Rebuild
echo ========================================
echo.

echo Stopping existing containers...
docker-compose -f docker-compose.dev.yml down

echo.
echo Removing old images to force rebuild...
docker-compose -f docker-compose.dev.yml rm -f

echo.
echo Rebuilding containers (this may take a few minutes)...
docker-compose -f docker-compose.dev.yml build --no-cache

echo.
echo Starting containers...
docker-compose -f docker-compose.dev.yml up -d

echo.
echo ========================================
echo Docker containers are starting up!
echo ========================================
echo.
echo Frontend: http://localhost:80
echo Backend:  http://localhost:8080
echo PgAdmin:  http://localhost:5050
echo.
echo Checking container status...
docker-compose -f docker-compose.dev.yml ps

echo.
echo To view logs, run:
echo   docker-compose -f docker-compose.dev.yml logs -f
echo.
echo To stop containers, run:
echo   docker-compose -f docker-compose.dev.yml down
echo.
