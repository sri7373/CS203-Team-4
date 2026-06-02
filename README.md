# CS203-Team-4 - TARIFF System

[![CI Build](https://github.com/sri7373/CS203-Team-4/actions/workflows/ci.yml/badge.svg)](https://github.com/sri7373/CS203-Team-4/actions/workflows/ci.yml)
[![Deploy Production](https://github.com/sri7373/CS203-Team-4/actions/workflows/cd-production.yml/badge.svg)](https://github.com/sri7373/CS203-Team-4/actions/workflows/cd-production.yml)

**Trade Agreements Regulating Imports and Foreign Fees (TARIFF)**
SMU CS students for the CS203 Collaborative Software Development module. The system enables users to define import tariffs and additional fees, and calculate them for specific industries across different countries at any given time.

## Features

- Dynamic tariff rate calculations
- Multi-country and multi-industry support
- Real-time news integration
- Admin dashboard for tariff management
- User authentication with JWT
- RESTful API with comprehensive documentation
- Responsive React frontend
- CI/CD pipeline with automated deployments and coverage enforcement
- AWS RDS database integration
- Docker containerization

## Tech Stack

### Backend
- **Java 17** with Spring Boot 3.3.4
- **PostgreSQL 15** (AWS RDS)
- **Spring Security** with JWT authentication
- **Flyway** for database migrations
- **Spring Data JPA** for ORM
- **Maven** for dependency management

### Frontend
- **React 18** with Vite
- **Framer Motion** for animations
- **React Router** for navigation
- **Axios** for API calls

### Infrastructure
- **Docker** & Docker Compose
- **AWS ECR** for container registry
- **AWS RDS** for PostgreSQL database
- **AWS EC2** for application hosting
- **GitHub Actions** for CI/CD

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Node.js 18+ (for local frontend development)
- Java 17+ (for local backend development)
- AWS account (for production deployment)

### Local Development

```bash
# Clone the repository
git clone https://github.com/sri7373/CS203-Team-4.git
cd CS203-Team-4

# Run with Docker Compose
docker-compose -f docker-compose.dev.yml up -d

# Access the application
# Frontend: http://localhost:8081
# Backend API: http://localhost:8080
# PgAdmin: http://localhost:5050
```

Or use the convenient script:
```bash
chmod +x scripts/local-deploy.sh
./scripts/local-deploy.sh
```

### Production Deployment

See our comprehensive guides:
- **[CI/CD Operations Overview](./docs/CI-CD-OVERVIEW.md)** – What each workflow does and how to use the artifacts
- **[Full CI/CD Setup Documentation](./docs/CI-CD-SETUP.md)** – Detailed setup instructions
- **[Docker Documentation](./docs/DOCKER.md)** – Docker configuration details

## Documentation

- [API Route Documentation](./docs/API_ROUTE_DOCUMENTATION.md)
- [Backend Architecture](./docs/backend-architecture.md)
- [Calculations Documentation](./docs/calculations.md)
- [NewsData Architecture](./docs/newsdata-architecture.md)
- [CI/CD Pipeline Setup](./docs/CI-CD-SETUP.md)
- [Docker Setup Guide](./docs/DOCKER.md)

## CI/CD Pipeline

Our automated pipeline includes:

### Continuous Integration (CI)
- Automated testing for backend and frontend
- Code quality checks and linting
- Security vulnerability scanning
- Docker image builds
- Test coverage reports with JaCoCo gates (currently 10 % branch / 25 % line)

### Continuous Deployment (CD)
- **Production**: Auto-deploy on push to `main` or tagged releases
- Version tags supported (v*.*.*)
- Blue-green deployment strategy
- Automated database migrations
- Health checks and smoke tests
- Automatic rollback on failure

### Deployment Workflow
```
Feature Branch -> PR -> Develop/Main -> Production
```


## Branch Strategy

- `main` - Production-ready code (stable releases)
- `develop` - Development branch (auto-deploys to production)
- `feature/*` - New features
- `hotfix/*` - Critical production fixes



