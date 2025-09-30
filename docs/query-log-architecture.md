# Query Log Architecture

## Overview
The application records tariff-related activity (calculations, searches, PDF exports) in the `query_log` table. The end-to-end design spans three layers: backend services that capture events, the database schema that persists them, and frontend clients that display per-user history. Each layer plays a distinct role to deliver secure, auditable logging.

## Backend Architecture

### Flow
- `TariffService` calls `QueryLogService.log(type, params)` whenever a user performs an action that should be recorded.
- `QueryLogService` resolves the acting user. It first checks the Spring Security context populated by `JwtAuthFilter`; if unavailable it inspects the current HTTP request for a Bearer token via `JwtService`.
- After resolving the `User`, the service persists a `QueryLog` entity through `QueryLogRepository`.
- `QueryLogController` exposes `/api/query-logs` endpoints. Each endpoint retrieves the current user from `QueryLogService.getCurrentUser()` and only returns rows associated with that user. Requests without valid JWTs yield 401, and cross-account access returns 403.
- `SecurityConfig` removes query-log routes from the anonymous allow-list so all logging APIs require authentication.

### Why this structure?
- **Single responsibility**: Controllers handle transport and authorisation, services encapsulate logging/business rules, and repositories manage SQL/JPA queries.
- **Security**: With JWT enforcement and per-user filtering, even if a client bug occurs the backend prevents cross-user exposure.
- **Reusability**: Any future feature that needs to log activity simply calls `QueryLogService.log`, inheriting the same resolution logic.

## Database Architecture

### Schema Highlights
- `query_log` columns: `id`, `user_id` (FK to `users`), `type`, `params` (text), `created_at` (timestamp).
- Foreign key ensures referential integrity; indexed timestamps allow efficient “latest first” ordering.

### Rationale
- Storing `user_id` enables simple `WHERE user_id = ?` filtering, pushing multi-tenant isolation to the database layer.
- Keeping raw `params` avoids frequent schema migrations; the service can parse JSON-like payloads when needed.

## Frontend Architecture

### Client Behaviour
- Axios configuration (`frontend/src/services/api.js`) adds the JWT bearer token to every request—this includes the PDF download after switching it to `api.post()`.
- `QueryLogsPage.jsx` simply hits `/api/query-logs` and renders the returned list. Because the backend already filters by user, the UI can display results without additional checks.
- Other pages (e.g., `CalculatePage.jsx`) focus on business actions; logging remains transparent to the client.

### Why this architecture?
- **Minimal trust in clients**: Logging happens on the server after business logic, so users cannot spoof log entries by manipulating the browser.
- **Consistency**: A single Axios interceptor guarantees the Authorization header is always present, keeping logs attributable.
- **Thin presentation layer**: The frontend consumes a clean API and can evolve independently of logging rules.

## Design Principles
- **Security-first**: JWT-enforced APIs and repository-level filtering prevent accidental data leaks.
- **Audit-ready**: Logs include user identity and timestamp so admins can reconstruct actions during investigations.
- **Maintainable**: Clear boundaries between controller/service/repository/database/frontend make it easy to extend logging or add admin dashboards later.

## Future Enhancements
- Add admin-only endpoints for aggregated or all-user views with explicit role checks.
- Transition `params` to a JSON column (where supported) for richer server-side filtering.
- Build reporting dashboards that summarise activity trends (counts per action, per day, etc.).

---
This architecture aligns the backend, database, and frontend to provide reliable, secure, per-user query logging without imposing heavy requirements on the client.