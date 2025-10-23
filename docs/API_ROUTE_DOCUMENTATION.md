# API Route Design and Implementation

## Overview

This document describes the REST API architecture for the Tariff Management System. The backend is built using Spring Boot and follows a layered architecture with controllers handling HTTP requests, services implementing business logic, and repositories managing data persistence.

## Base URL

All API routes are prefixed with `/api`

## Authentication & Security

### Authentication Mechanism
- **JWT (JSON Web Token)** based authentication
- Token-based stateless sessions (`SessionCreationPolicy.STATELESS`)
- Custom `JwtAuthFilter` intercepts requests before `UsernamePasswordAuthenticationFilter`
- Passwords encrypted using `BCryptPasswordEncoder`

### Security Configuration
- **Public Endpoints**: `/api/auth/**`, `/api/trade/**`, Swagger documentation
- **Authenticated Endpoints**: All other endpoints require valid JWT token
- **Role-based Authorization**: Uses `@PreAuthorize` annotations with `ROLE_USER` and `ROLE_ADMIN`
- **CORS**: Enabled via `CorsConfig` for cross-origin requests

## API Endpoints

### 1. Authentication Routes (`/api/auth`)

**Controller**: `AuthController.java:22`

#### POST /api/auth/login
- **Description**: Authenticate user and generate JWT token
- **Request Body**: `AuthRequest` (username, password)
- **Response**: `AuthResponse` (token, username, role)
- **Status Codes**:
  - 200: Success
  - 401: Invalid credentials

#### POST /api/auth/register
- **Description**: Register new user account
- **Request Body**: `RegisterRequest` (username, email, password, role)
- **Response**: `AuthResponse` (token, username, role)
- **Validation**:
  - Normalizes username and email (trim, lowercase for email)
  - Checks for duplicate username/email
  - Defaults to `Role.USER` if not specified
- **Status Codes**:
  - 200: Success
  - 400: Invalid input (blank fields)
  - 409: Conflict (username/email already exists)

---

### 2. Tariff Routes (`/api/tariffs`)

**Controller**: `TariffController.java:28`

#### POST /api/tariffs/calculate
- **Description**: Calculate tariff costs for a product shipment
- **Authorization**: `USER` or `ADMIN`
- **Request Body**: `TariffCalcRequest`
  - `originCountryCode` (required): Origin country code
  - `destinationCountryCode` (required): Destination country code
  - `productCategoryCode` (required): Product category code
  - `declaredValue` (required): Product value > 0
  - `date` (optional): Effective date (ISO format), defaults to today
- **Query Parameters**:
  - `includeSummary` (boolean, default: true): Include AI-generated summary
- **Response**: `TariffCalcResponse`
  - Calculated tariff amount, additional fees, total cost
  - AI-generated HTML summary explaining tariff structure
- **Business Logic** (TariffService.java:70):
  - Validates country codes and product categories
  - Finds applicable tariff rates based on date
  - Calculates: `total = declaredValue + (declaredValue * baseRate) + additionalFee`
  - Logs query to database for analytics
  - Generates AI summary using Gemini API
- **Status Codes**:
  - 200: Success
  - 400: Invalid request (missing fields, invalid values)
  - 404: No applicable tariff rate found

#### GET /api/tariffs/rates
- **Description**: Search tariff rates by origin, destination, and category
- **Authorization**: `USER` or `ADMIN`
- **Query Parameters**:
  - `origin` (optional): Origin country code
  - `destination` (optional): Destination country code
  - `category` (optional): Product category code
- **Response**: List of `TariffRateDto`
- **Status Codes**:
  - 200: Success
  - 400: Invalid country/category code

#### POST /api/tariffs/calculate/summary
- **Description**: Generate AI summary for existing calculation response
- **Authorization**: `USER` or `ADMIN`
- **Request Body**: `TariffCalcResponse`
- **Response**: `{ "aiSummary": "<HTML content>" }`
- **Status Codes**:
  - 200: Success
  - 400: Invalid response object

#### POST /api/tariffs/calculate/pdf
- **Description**: Generate PDF report for tariff calculation
- **Authorization**: `USER` or `ADMIN`
- **Request Body**: `TariffCalcRequest`
- **Response**: PDF file (binary)
- **Headers**: `Content-Disposition: attachment; filename=tariff-report.pdf`
- **Content-Type**: `application/pdf`
- **Status Codes**:
  - 200: Success with PDF
  - 400: Invalid calculation request

#### GET /api/tariffs
- **Description**: Get all tariff rates (Admin only)
- **Authorization**: `ADMIN`
- **Response**: List of all `TariffRateDto`
- **Status Codes**:
  - 200: Success
  - 403: Forbidden (non-admin user)

#### POST /api/tariffs
- **Description**: Create new tariff rate (Admin only)
- **Authorization**: `ADMIN`
- **Request Body**: `TariffRateDtoPost`
  - All fields required: originCountryCode, destinationCountryCode, productCategoryCode, baseRate, additionalFee, effectiveFrom
  - effectiveTo is optional
- **Response**: Created `TariffRateDto`
- **Status Codes**:
  - 200: Success
  - 400: Invalid input
  - 403: Forbidden

#### PUT /api/tariffs/{id}
- **Description**: Update existing tariff rate (Admin only)
- **Authorization**: `ADMIN`
- **Path Variable**: `id` - Tariff rate ID
- **Request Body**: `TariffRateDtoPost` (partial updates supported)
- **Response**: Updated `TariffRateDto`
- **Status Codes**:
  - 200: Success
  - 400: Invalid input
  - 403: Forbidden
  - 404: Tariff not found

#### DELETE /api/tariffs/{id}
- **Description**: Delete tariff rate (Admin only)
- **Authorization**: `ADMIN`
- **Path Variable**: `id` - Tariff rate ID
- **Response**: No content
- **Status Codes**:
  - 204: Success (No Content)
  - 403: Forbidden
  - 404: Tariff not found

---

### 3. Reference Data Routes (`/api/reference`)

**Controller**: `ReferenceController.java:18`

#### GET /api/reference/countries
- **Description**: Get list of available countries
- **Authorization**: `USER` or `ADMIN`
- **Response**: List of `ReferenceOptionDto` (code, name)
  - Supported: SGP, USA, CHN, MYS, IDN
  - Names resolved from database with fallback to hardcoded values
- **Status Codes**:
  - 200: Success

#### GET /api/reference/product-categories
- **Description**: Get list of available product categories
- **Authorization**: `USER` or `ADMIN`
- **Response**: List of `ReferenceOptionDto` (code, name)
  - Supported: STEEL, ELEC, FOOD
  - Names resolved from database with fallback
- **Status Codes**:
  - 200: Success

---

### 4. Trade Analytics Routes (`/api/trade`)

**Controller**: `TradeAnalyticsController.java:11`

#### GET /api/trade/insights
- **Description**: Get trade insights for a specific country
- **Authorization**: Public (no authentication required)
- **Query Parameters**:
  - `country` (required): Country code (e.g., "SGP")
- **Response**: `CountryTradeInsightsDto`
  - Top import/export partners
  - Top import/export products
  - Trade metrics and analytics
- **Status Codes**:
  - 200: Success
  - 400: Invalid country code

---

### 5. Query Log Routes (`/api/query-logs`)

**Controller**: `QueryLogController.java:23`

#### GET /api/query-logs
- **Description**: Get all query logs for current authenticated user
- **Authorization**: Authenticated user (token required)
- **Response**: List of query log objects with detailed metadata
  - Includes user info, action type, parameters, results
  - Filters to show only CALCULATE-type logs for the user
- **Status Codes**:
  - 200: Success
  - 401: Unauthorized

#### GET /api/query-logs/user/{userId}
- **Description**: Get query logs for specific user
- **Authorization**: Authenticated user (must match userId)
- **Path Variable**: `userId` - User ID
- **Response**: List of query logs
- **Status Codes**:
  - 200: Success
  - 401: Unauthorized
  - 403: Forbidden (accessing another user's logs)

#### GET /api/query-logs/test
- **Description**: Test database connection and get log count
- **Authorization**: Authenticated user
- **Response**: Connection status, timestamp, total logs for user
- **Status Codes**:
  - 200: Success
  - 401: Unauthorized
  - 500: Database error

#### GET /api/query-logs/debug
- **Description**: Get detailed debug information about user's logs
- **Authorization**: Authenticated user
- **Response**: Total logs, sample log details (up to 5)
- **Status Codes**:
  - 200: Success
  - 401: Unauthorized

#### GET /api/query-logs/raw
- **Description**: Get raw query log data for current user
- **Authorization**: Authenticated user
- **Response**: List of raw log entries (id, createdAt, userId)
- **Status Codes**:
  - 200: Success
  - 401: Unauthorized

---

### 6. Debug Routes (`/api/tariffs/debug`)

**Controller**: `DebugController.java:18`

#### GET /api/tariffs/debug/auth
- **Description**: Debug authentication and JWT token validation
- **Authorization**: Public (for debugging purposes)
- **Response**: Object containing:
  - Authorization header value
  - Extracted username from token
  - Token validity status
  - Security context authentication details
- **Status Codes**:
  - 200: Success
  - 500: Error during debug

#### GET /api/tariffs/debug/simple
- **Description**: Simple authentication debug information
- **Authorization**: Public
- **Response**: Authorization header, security authentication status
- **Status Codes**:
  - 200: Success
  - 500: Error during debug

---

## Error Handling

**Global Exception Handler**: `GlobalExceptionHandler.java:21`

### Error Response Format
All errors return standardized `ErrorResponse`:
```json
{
  "timestamp": "2025-10-06T...",
  "status": 400,
  "error": "Bad Request",
  "message": "Descriptive error message",
  "path": "/api/tariffs/calculate"
}
```

### Exception Mappings

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `TariffNotFoundException` | 404 | Tariff rate not found for given criteria |
| `InvalidTariffRequestException` | 400 | Invalid request parameters or validation failure |
| `MethodArgumentNotValidException` | 400 | Bean validation failed (e.g., @NotBlank, @DecimalMin) |
| `DataIntegrityViolationException` | 409 | Database constraint violation (duplicate username/email) |
| `IllegalArgumentException` | 400 | Invalid argument passed to service |
| `AuthenticationException` | 401 | Invalid credentials |
| `AccessDeniedException` | 403 | Insufficient permissions |
| `RuntimeException` | 500 | Unexpected runtime error |
| `Exception` | 500 | Generic unexpected error |

---

## Data Transfer Objects (DTOs)

### TariffCalcRequest
```java
{
  originCountryCode: String (required, not blank)
  destinationCountryCode: String (required, not blank)
  productCategoryCode: String (required, not blank)
  declaredValue: Double (required, > 0)
  date: String (optional, ISO format)
}
```

### TariffCalcResponse
```java
{
  originCountryCode: String
  destinationCountryCode: String
  productCategoryCode: String
  effectiveDate: String
  declaredValue: BigDecimal
  baseRate: BigDecimal
  tariffAmount: BigDecimal
  additionalFee: BigDecimal
  totalCost: BigDecimal
  notes: String
  aiSummary: String (HTML content)
}
```

### TariffRateDto
```java
{
  id: Long
  originCountryCode: String
  destinationCountryCode: String
  productCategoryCode: String
  baseRate: BigDecimal
  additionalFee: BigDecimal
  effectiveFrom: LocalDate
  effectiveTo: LocalDate
}
```

### AuthRequest
```java
{
  username: String
  password: String
}
```

### AuthResponse
```java
{
  token: String (JWT)
  username: String
  role: String
}
```

### RegisterRequest
```java
{
  username: String
  email: String
  password: String
  role: Role (optional, defaults to USER)
}
```

---

## Design Patterns & Architecture

### Layered Architecture
1. **Controllers** - Handle HTTP requests/responses, input validation
2. **Services** - Business logic, transaction management
3. **Repositories** - Data access layer using Spring Data JPA
4. **DTOs** - Data transfer between layers
5. **Entities** - JPA entities mapped to database tables

### Key Design Features

#### 1. Role-Based Access Control (RBAC)
- Method-level security using `@PreAuthorize`
- Two roles: `USER` and `ADMIN`
- Admin-only operations: CRUD on tariff rates
- User operations: Calculate tariffs, search rates, view logs

#### 2. Request Validation
- Bean Validation (JSR-380) with annotations like `@NotBlank`, `@NotNull`, `@DecimalMin`
- Custom validation in service layer
- Normalized inputs (trim whitespace, uppercase country codes)

#### 3. Query Logging
- `QueryLogService` logs all tariff calculations and searches
- Stores user, action type, parameters, and results
- Used for analytics and user history tracking

#### 4. AI Integration
- `GeminiClient` generates AI summaries for tariff calculations
- Explains trade policies and provides actionable insights
- HTML output sanitized using Jsoup with allowlist
- Graceful degradation if AI service unavailable

#### 5. PDF Generation
- Uses OpenPDF library to generate tariff reports
- Includes all calculation details and metadata
- Returns as downloadable attachment

#### 6. Exception Handling
- Centralized error handling with `@RestControllerAdvice`
- Consistent error response format
- Detailed logging for debugging
- User-friendly error messages

#### 7. Security Best Practices
- Stateless JWT authentication
- Password hashing with BCrypt
- CORS configuration for frontend integration
- Debug endpoints for troubleshooting (should be disabled in production)

---

## Database Schema References

### Core Entities
- **User**: Authenticated users (id, username, email, password, role)
- **Country**: Trade countries (code, name)
- **ProductCategory**: Product types (code, name)
- **TariffRate**: Tariff configurations (origin, destination, category, rates, dates)
- **QueryLog**: User query history (type, params, result, timestamps)
- **TradeFlow**: Trade analytics data (country, partner, product, metrics)

---

## API Workflow Examples

### 1. User Registration & Authentication Flow
```
1. POST /api/auth/register → Register new user → Get JWT token
2. Include token in Authorization header: "Bearer <token>"
3. Access protected endpoints
```

### 2. Tariff Calculation Flow
```
1. GET /api/reference/countries → Get available countries
2. GET /api/reference/product-categories → Get product categories
3. POST /api/tariffs/calculate → Calculate tariff with AI summary
4. Optional: POST /api/tariffs/calculate/pdf → Download PDF report
5. GET /api/query-logs → View calculation history
```

### 3. Admin Tariff Management Flow
```
1. POST /api/auth/login (as admin) → Get admin JWT token
2. GET /api/tariffs → View all tariff rates
3. POST /api/tariffs → Create new tariff rate
4. PUT /api/tariffs/{id} → Update existing rate
5. DELETE /api/tariffs/{id} → Remove rate
```

---

## Notes

- All timestamps use ISO-8601 format
- Monetary values use `BigDecimal` for precision
- Country codes follow ISO 3166-1 alpha-3 (3-letter codes)
- Dates use `LocalDate` (yyyy-MM-dd format)
- API follows RESTful conventions
- Transactional integrity maintained with `@Transactional`
- OpenAPI/Swagger documentation available at `/swagger-ui.html`
