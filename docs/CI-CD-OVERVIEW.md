# CI/CD Operational Overview

This document explains what each automated workflow does today, what artifacts it produces, and how to interpret the results. Use it alongside the detailed setup guide (`docs/CI-CD-SETUP.md`) when maintaining or extending the pipeline.

## Workflow Summary

| Workflow | File | Trigger | Purpose |
| --- | --- | --- | --- |
| Continuous Integration | `.github/workflows/ci.yml` | Every push/PR to `main`, `develop`, `feature/*` | Validate backend + frontend, collect coverage, build images, run security scan |
| Production Deploy | `.github/workflows/cd-production.yml` | Push to `main`, release tags | Build/push images, run zero-downtime deploy + smoke tests |

> Staging deployment automations follow the same pattern as production but target the staging environment (`cd-staging.yml`).

## CI Job Details

### `backend-test`
- Spins up PostgreSQL 15 in a service container.
- Runs `mvn -B clean verify`, which now includes:
  - Unit/integration tests.
  - JaCoCo branch/line coverage enforcement (current thresholds: 40 % branch, 55 % line).
  - XML + HTML coverage reporting (`backend/target/jacoco*.xml`, `backend/target/site/jacoco`).
- Uploads two artifacts:
  - `backend-test-results` – Surefire/JUnit XML.
  - `backend-coverage-report` – JaCoCo HTML + XML output.

### `frontend-test`
- Uses Node.js 18 cache-aware install (`npm ci`).
- Runs `npm run lint` (non-blocking), `npm test`, and `npm run build`.
- Uploads `frontend-build` artifact for downstream jobs or manual inspection.

### `docker-build`
- Depends on both test jobs.
- Builds backend and frontend Docker images using Buildx with GitHub Actions cache but does not push (safety gate for PRs).

### `security-scan`
- Runs Trivy in filesystem mode to surface dependency/CVE issues without failing the pipeline (exit code forced to 0).

## Coverage & Quality Gates

- JaCoCo thresholds live in `backend/pom.xml` (`jacoco.min.branch.coverage`, `jacoco.min.line.coverage`). Adjust them there as the suite matures.
- Any failure in `mvn verify` (tests or coverage) stops the pipeline before Docker builds, ensuring only healthy changes proceed.
- Download coverage artifacts from the workflow run page (`Actions ▸ CI - Build and Test ▸ <run> ▸ Artifacts`) to inspect HTML reports locally.

## Deployment Pipeline Highlights

- Production workflow builds fresh images, pushes to AWS ECR, performs blue/green deployment with automatic rollback if health checks or smoke tests fail.
- Backups are taken prior to deploy; logs and backup directories live on the EC2 host as described in `docs/CI-CD-SETUP.md`.

## Operating Tips

- **Rerunning CI:** Use the “Re-run jobs” button when Maven Central or npm has transient outages.
- **Artifact retention:** GitHub retains artifacts for 90 days by default—download coverage reports promptly if you need offline audits.
- **Raising coverage:** Add backend tests, run `mvn clean verify`, confirm JaCoCo passes locally before pushing to avoid pipeline failures.

## Useful Links

- [CI workflow file](../.github/workflows/ci.yml)
- [Coverage configuration (`pom.xml` excerpt)](../backend/pom.xml)
- [Setup & deployment guide](./CI-CD-SETUP.md)
