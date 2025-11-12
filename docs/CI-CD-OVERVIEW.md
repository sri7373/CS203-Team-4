# CI/CD Operational Overview

This overview summarizes what each workflow currently does, which artifacts it produces, and how to interpret the results. Pair it with the detailed setup guide (`docs/CI-CD-SETUP.md`) when you need deeper configuration steps.

## Workflow Summary

| Workflow | File | Trigger | Purpose |
| --- | --- | --- | --- |
| Continuous Integration | `.github/workflows/ci.yml` | Every push/PR to `main`, `develop`, `feature/*` | Validate backend + frontend, collect coverage, build Docker images, run Trivy |
| Production Deploy | `.github/workflows/cd-production.yml` | Push to `main`, release tags | Build/push images, run zero-downtime deploy + smoke tests |

> A staging deployment workflow (`cd-staging.yml`) mirrors the production flow but targets the staging environment.

## CI Job Details

### `backend-test`
- Spins up PostgreSQL 15 as a service container.
- Runs `mvn -B clean verify`, which includes:
  - Unit/integration tests (repository slices connect to the Postgres service; other tests rely on Mockito/H2).
  - JaCoCo branch/line coverage enforcement (current thresholds: **10 % branch**, **25 % line**).
  - XML + HTML coverage reporting (`backend/target/jacoco*.xml`, `backend/target/site/jacoco`).
- Uploads:
  - `backend-test-results` — Surefire/JUnit XML.
  - `backend-coverage-report` — JaCoCo HTML + XML output.

### `frontend-test`
- Uses Node.js 18 with cached `npm ci`.
- Runs `npm run lint` (best-effort), `npm test`, and `npm run build`.
- Uploads `frontend-build` (Vite `dist/`) for downstream inspection.

### `docker-build`
- Depends on both test jobs.
- Builds backend and frontend images via Buildx using GitHub cache (no push for PRs).

### `security-scan`
- Runs Trivy in filesystem mode to surface dependency/CVE issues (non-blocking via `exit-code: 0`).

## Coverage & Quality Gates

- Thresholds live in `backend/pom.xml` (`jacoco.min.branch.coverage`, `jacoco.min.line.coverage`). They are intentionally low (0.10 / 0.25) until the suite matures — raise them as coverage increases.
- `mvn verify` fails if tests or coverage gates fail, preventing Docker builds from running on broken changes.
- Download coverage artifacts from the workflow run (`Actions -> CI - Build and Test -> <run> -> Artifacts`) to review HTML reports locally.

## Deployment Pipeline Highlights

- Production deploy workflow builds fresh images, pushes to AWS ECR, performs blue/green rollout, runs smoke tests, and rolls back automatically on failure. Backups are taken before the cutover.
- The staging workflow follows the same steps but targets the staging infrastructure.

## Operating Tips

- **Rerun CI** when Maven Central/npm flake out — GitHub Actions exposes a “Re-run jobs” button.
- **Artifact retention** is 90 days by default; download coverage/test artifacts promptly if you need offline reviews.
- **Improving coverage**: add backend tests, run `mvn clean verify`, and only raise the thresholds once the suite consistently beats the new target.

## Useful Links

- [CI workflow](../.github/workflows/ci.yml)
- [Coverage configuration (`backend/pom.xml`)](../backend/pom.xml)
- [Setup & deployment guide](./CI-CD-SETUP.md)
