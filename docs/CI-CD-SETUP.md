# CI/CD Pipeline Setup Guide

## Overview
This project uses GitHub Actions for CI/CD with deployment to AWS infrastructure using Docker containers and AWS RDS for the database.

## Pipeline Structure

### 1. **Continuous Integration (CI)** - `.github/workflows/ci.yml`
Runs on every push and pull request to `main`, `develop`, and feature branches.

**What it does:**
- Runs backend tests against PostgreSQL via `mvn -B clean verify` (includes JaCoCo coverage gate and artifacts)
- Runs frontend linting/tests and builds the production bundle
- Builds backend/frontend Docker images (cache-only)
- Performs security scanning with Trivy
- Publishes Surefire + JaCoCo artifacts for download

### 2. **Staging Deployment** - `.github/workflows/cd-staging.yml`
Automatically deploys to staging when code is pushed to `develop` branch.

**What it does:**
- Builds and pushes Docker images to AWS ECR
- Deploys to the staging EC2/ECS environment
- Runs database migrations
- Performs health checks before marking the deploy successful

### 3. **Production Deployment** - `.github/workflows/cd-production.yml`
Deploys to production when code is pushed to `main` or when a version tag is created.

**What it does:**
- Builds and pushes production Docker images to AWS ECR
- Creates an application/DB backup before deployment
- Executes blue-green deployment for zero downtime
- Runs database migrations
- Performs smoke tests
- Rolls back automatically on failure
- Creates GitHub releases for version tags

### Coverage & Test Artifacts

- JaCoCo thresholds (currently 40% branch / 55% line) live in `backend/pom.xml`.
- Every CI run uploads `backend-test-results` and `backend-coverage-report` artifacts. Download them from the workflow run (Actions -> CI -> Artifacts) to inspect JUnit XML or open `target/site/jacoco/index.html`.
- If coverage fails locally or in CI, run `mvn clean verify` in `backend/` to reproduce; JaCoCo output lives under `backend/target`.

## Prerequisites

### AWS Setup

#### 1. Create ECR Repositories
```bash
aws ecr create-repository --repository-name tariff-backend --region ap-southeast-1
aws ecr create-repository --repository-name tariff-frontend --region ap-southeast-1
```

#### 2. Create RDS Database (if not already done)
```bash
aws rds create-db-instance \
  --db-instance-identifier tariff-db-prod \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username admin \
  --master-user-password <YOUR_SECURE_PASSWORD> \
  --allocated-storage 20 \
  --vpc-security-group-ids <YOUR_SECURITY_GROUP> \
  --db-name tariff_prod \
  --backup-retention-period 7 \
  --region ap-southeast-1
```

#### 3. Create EC2 Instance (or use existing)
- Launch Ubuntu 22.04 LTS instance
- Install Docker and Docker Compose
- Configure security groups to allow HTTP/HTTPS
- Set up Elastic IP for consistent addressing

```bash
# On your EC2 instance
sudo apt update
sudo apt install -y docker.io docker-compose awscli
sudo usermod -aG docker ubuntu
sudo systemctl enable docker
sudo systemctl start docker
```

### GitHub Secrets Configuration

Go to your repository -> Settings -> Secrets and variables -> Actions

Add the following secrets:

#### AWS Credentials
- `AWS_ACCESS_KEY_ID` - Your AWS access key
- `AWS_SECRET_ACCESS_KEY` - Your AWS secret key

#### Staging Environment
- `STAGING_HOST` - IP address of staging EC2 instance
- `STAGING_USER` - SSH username (e.g., `ubuntu`)
- `STAGING_SSH_KEY` - Private SSH key for staging server
- `STAGING_URL` - Staging application URL (e.g., `http://staging.example.com`)

#### Production Environment
- `PRODUCTION_HOST` - IP address of production EC2 instance
- `PRODUCTION_USER` - SSH username (e.g., `ubuntu`)
- `PRODUCTION_SSH_KEY` - Private SSH key for production server
- `PRODUCTION_URL` - Production application URL (e.g., `https://example.com`)

#### Database Secrets (for production/staging)
- `POSTGRES_DB` - Database name
- `POSTGRES_USER` - Database username
- `POSTGRES_PASSWORD` - Database password
- `JWT_SECRET` - JWT secret key for authentication
- `JWT_EXPIRATION_MS` - JWT expiration time in milliseconds

## Deployment Flow

### Development Workflow
```
1. Create feature branch from develop
2. Make changes and push
3. CI pipeline runs automatically (tests, builds, scans)
4. Create PR to develop
5. After PR approval and merge -> auto-deploy to staging
6. Test in staging environment
7. Create PR from develop to main
8. After PR approval and merge -> auto-deploy to production
```

### Emergency Hotfix Workflow
```
1. Create hotfix branch from main
2. Make critical fix
3. Push and create PR to main
4. Fast-track approval
5. Merge triggers production deployment
6. Backport to develop
```

### Version Release Workflow
```
1. Ensure develop is stable and tested in staging
2. Merge develop to main
3. Create version tag:
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
4. Production deployment triggers with versioned images
5. GitHub release created automatically
```

## Environment Setup on EC2

Create `.env` file on your EC2 instances:

```bash
# /home/ubuntu/app/.env

# Database Configuration (AWS RDS)
POSTGRES_DB=tariff_prod
POSTGRES_USER=admin
POSTGRES_PASSWORD=<YOUR_SECURE_PASSWORD>

# JWT Configuration
JWT_SECRET=<YOUR_JWT_SECRET>
JWT_EXPIRATION_MS=86400000

# API Keys
GEMINI_API_KEY=<YOUR_GEMINI_KEY>
NEWSDATA_API_KEY=<YOUR_NEWSDATA_KEY>

# Database URL (for backend)
DB_URL=jdbc:postgresql://<RDS_ENDPOINT>:5432/tariff_prod
DB_USERNAME=admin
DB_PASSWORD=<YOUR_SECURE_PASSWORD>
```

## Monitoring and Rollback

### View Logs
```bash
# On EC2 instance
cd /home/ubuntu/app
docker-compose -f docker-compose.prod.yml logs -f
```

### Manual Rollback
```bash
# On EC2 instance
cd /home/ubuntu/app/backups
ls -la  # Find backup directory
cp <BACKUP_DIR>/docker-compose.prod.yml ../
cd ..
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

### Health Checks
- Backend: `https://your-domain.com/actuator/health`
- Frontend: `https://your-domain.com/`
- Database: Check RDS console

## Troubleshooting

### Build Failures
1. Check GitHub Actions logs
2. Verify secrets are correctly set
3. Ensure AWS credentials have proper permissions

### Deployment Failures
1. SSH into EC2 instance
2. Check Docker logs: `docker-compose logs`
3. Verify RDS connectivity
4. Check security group rules

### Database Migration Issues
1. SSH into EC2 instance
2. Check Flyway migration status
3. Manually run migrations if needed:
   ```bash
   docker-compose exec backend java -jar app.jar --migrate
   ```

## Security Best Practices

1. Use AWS Secrets Manager (or Parameter Store) for sensitive data
2. Rotate SSH keys regularly and disable unused accounts
3. Keep RDS/EC2 inside a VPC with restricted security groups
4. Enforce HTTPS end-to-end (Let's Encrypt or ACM)
5. Run security scans frequently (Trivy in CI plus periodic AWS Inspector)
6. Keep dependencies updated (dependabot + scheduled upgrades)
7. Monitor AWS CloudWatch metrics/logs for anomalies

## Cost Optimization

1. Use AWS Free Tier when possible
2. Stop staging environment when not in use
3. Use spot instances for non-critical environments
4. Set up auto-scaling for production
5. Monitor RDS usage and downsize if needed
6. Clean up old Docker images regularly (automated in pipeline)

## Next Steps

1. Set up SSL certificates with AWS Certificate Manager
2. Configure CloudFront for CDN/offloading
3. Set up AWS CloudWatch alarms and dashboards
4. Implement automated backups for RDS
5. Add Slack/Discord notifications for deployments
6. Set up monitoring with Prometheus/Grafana
7. Implement feature flags for gradual rollouts

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [AWS ECR Documentation](https://docs.aws.amazon.com/ecr/)
- [AWS RDS Documentation](https://docs.aws.amazon.com/rds/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)












