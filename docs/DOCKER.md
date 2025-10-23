# TARIFFSHERIFF Docker Deployment Guide

## Architecture Overview

Your Docker setup properly splits into:

- **Frontend**: React app served by Nginx (port 80)
- **Backend**: Spring Boot API (port 8080, internal)
- **Database**: PostgreSQL (local dev) or AWS RDS (production)

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- `.env` file with required variables (copy from `.env.example`)

## Quick Start

### Development Environment (PostgreSQL)

```bash
# Start all services
docker-compose -f docker-compose.dev.yml up -d

# Rebuild if there's changes
docker-compose -f docker-compose.dev.yml up -d --build

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop services
docker-compose -f docker-compose.dev.yml down
```

**Access:**

- Frontend: http://localhost:5173 (Vite dev server)
- Backend API: http://localhost:8080
- PgAdmin: http://localhost:5050
- PostgreSQL: localhost:5432

### Production Environment (with AWS RDS)

```bash
# Build and start
docker-compose -f docker-compose.prod.yml up -d --build

# View logs
docker-compose -f docker-compose.prod.yml logs -f backend
docker-compose -f docker-compose.prod.yml logs -f frontend

# Stop services
docker-compose -f docker-compose.prod.yml down
```

**Access:**

- Frontend: http://localhost:80
- Backend API: http://localhost:80/api (proxied through Nginx)

## Configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database
POSTGRES_DB=tariff_db
POSTGRES_USER=tariff_user
POSTGRES_PASSWORD=secure_password

# JWT
JWT_SECRET=your_256_bit_secret_key
JWT_EXPIRATION_MS=86400000

# PgAdmin (dev only)
PGADMIN_DEFAULT_EMAIL=admin@tariffsheriff.com
PGADMIN_DEFAULT_PASSWORD=admin_password
```

### AWS RDS Connection

Update `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-rds-endpoint.region.rds.amazonaws.com:5432/tariff_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

Or set environment variables in `docker-compose.prod.yml`:

```yaml
backend:
  environment:
    DB_URL: jdbc:postgresql://your-rds-endpoint.region.rds.amazonaws.com:5432/tariff_db
    DB_USERNAME: ${POSTGRES_USER}
    DB_PASSWORD: ${POSTGRES_PASSWORD}
```

## Running Individual Services

### Backend Only

```bash
cd backend
docker build -t tariffsheriff-backend .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/tariff_db \
  -e DB_USERNAME=tariff_user \
  -e DB_PASSWORD=password \
  tariffsheriff-backend
```

### Frontend Only

```bash
cd frontend
docker build -t tariffsheriff-frontend .
docker run -p 80:80 tariffsheriff-frontend
```

## Troubleshooting

### Backend can't connect to database

**Issue:** `Connection refused` or `Unknown host`

**Solutions:**

- For local dev: Use `host.docker.internal` instead of `localhost`
- For Docker network: Use service name (e.g., `database` or `db`)
- For AWS RDS: Ensure security groups allow inbound connections

### Frontend can't reach backend

**Issue:** API calls return 404 or CORS errors

**Solution:** Check `frontend/nginx.conf` proxy configuration:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
}
```

### Container keeps restarting

**Issue:** Service fails health check

**Debug:**

```bash
# Check logs
docker logs tariff_backend_prod

# Check health status
docker inspect --format='{{.State.Health.Status}}' tariff_backend_prod

# Exec into container
docker exec -it tariff_backend_prod sh
```

## Security Best Practices

1. **Never commit `.env` file** - It's in `.gitignore`
2. **Use AWS Secrets Manager** for production credentials
3. **Enable SSL/TLS** for production (use Nginx with Let's Encrypt)
4. **Restrict database access** to backend IP only
5. **Use non-root users** in containers (already configured)

## Building for Production

### Optimize Images

```bash
# Build with specific tags
docker build -t tariffsheriff-backend:1.0.0 ./backend
docker build -t tariffsheriff-frontend:1.0.0 ./frontend

# Push to registry (e.g., Docker Hub, AWS ECR)
docker tag tariffsheriff-backend:1.0.0 your-registry/tariffsheriff-backend:1.0.0
docker push your-registry/tariffsheriff-backend:1.0.0
```

### Deploy to Cloud

**AWS ECS/Fargate:**

- Upload images to ECR
- Create task definitions
- Configure load balancer
- Set environment variables in task definitions

**Azure Container Instances:**

```bash
az container create \
  --resource-group tariff-rg \
  --name tariffsheriff-backend \
  --image your-registry/tariffsheriff-backend:1.0.0 \
  --environment-variables DB_URL=... DB_USERNAME=... DB_PASSWORD=...
```

## Monitoring

### Health Checks

- Backend: http://localhost:8080/actuator/health
- Frontend: http://localhost/

### Container Stats

```bash
docker stats
```

### Logs

```bash
# Follow all logs
docker-compose logs -f

# Specific service
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend
```

## 🧹 Cleanup

```bash
# Stop and remove containers
docker-compose down

# Remove volumes (WARNING: deletes database data)
docker-compose down -v

# Remove all unused images
docker image prune -a

# Full cleanup
docker system prune -a --volumes
```

## 📚 Additional Resources

- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [AWS RDS Connection Guide](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.html)
