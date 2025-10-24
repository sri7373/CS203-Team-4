package com.smu.tariff;

import com.smu.tariff.tariff.TariffRateRepository;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for TariffController using REST Assured.
 * Tests follow BDD style with given-when-then pattern.
 * 
 * These tests run with a real HTTP server and full Spring context,
 * testing the entire request-response cycle including:
 * - Authentication and authorization
 * - Request validation
 * - Business logic
 * - Response formatting
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TariffControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TariffRateRepository tariffRateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String userJwtToken;
    private String adminJwtToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";

        // Clear test data if needed (be careful with this in production)
        // Note: Uncomment if you want to clear data between tests
        // tariffRateRepository.deleteAll();
        // userRepository.deleteAll();

        // Create test users if they don't exist
        setupTestUsers();

        // Authenticate and extract JWT tokens
        userJwtToken = authenticateAndGetToken("testuser", "password123");
        adminJwtToken = authenticateAndGetToken("testadmin", "admin123");
    }

    private void setupTestUsers() {
        // Create regular user if not exists
        if (!userRepository.existsByUsername("testuser")) {
            User user = new User(
                "testuser", 
                "testuser@example.com",
                passwordEncoder.encode("password123"),
                Role.USER
            );
            userRepository.save(user);
        }

        // Create admin user if not exists
        if (!userRepository.existsByUsername("testadmin")) {
            User admin = new User(
                "testadmin",
                "admin@example.com",
                passwordEncoder.encode("admin123"),
                Role.ADMIN
            );
            userRepository.save(admin);
        }
    }

    private String authenticateAndGetToken(String username, String password) {
        String requestBody = String.format(
            "{\"username\":\"%s\",\"password\":\"%s\"}", 
            username, 
            password
        );

        return given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");
    }

    // ==================== Calculate Endpoint Tests ====================

    @Test
    void calculateTariff_ShouldReturnValidResponse_WhenValidRequestProvided() {
        // Given: A valid tariff calculation request
        String requestBody = """
            {
                "originCountryCode": "SGP",
                "destinationCountryCode": "USA",
                "productCategoryCode": "ELEC",
                "declaredValue": 1000.0
            }
            """;

        // When: POST request is made to /api/tariff/calculate
        // Then: Should return 200 OK with valid response structure
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tariff/calculate")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("originCountryCode", equalTo("SGP"))
            .body("destinationCountryCode", equalTo("USA"))
            .body("productCategoryCode", equalTo("ELEC"))
            .body("declaredValue", notNullValue())
            .body("baseRate", notNullValue())
            .body("tariffAmount", notNullValue())
            .body("additionalFee", notNullValue())
            .body("totalCost", notNullValue())
            .body("effectiveDate", notNullValue());
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenOriginCountryIsNull() {
        // Given: Request with missing origin country
        String requestBody = """
            {
                "destinationCountryCode": "USA",
                "productCategoryCode": "ELEC",
                "declaredValue": 1000.0
            }
            """;

        // When: POST request is made
        // Then: Should return 400 Bad Request
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tariff/calculate")
        .then()
            .statusCode(400);
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenDeclaredValueIsNegative() {
        // Given: Request with negative declared value
        String requestBody = """
            {
                "originCountryCode": "SGP",
                "destinationCountryCode": "USA",
                "productCategoryCode": "ELEC",
                "declaredValue": -100.0
            }
            """;

        // When: POST request is made
        // Then: Should return 400 Bad Request
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tariff/calculate")
        .then()
            .statusCode(400);
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenInvalidCountryCode() {
        // Given: Request with invalid country code
        String requestBody = """
            {
                "originCountryCode": "INVALID",
                "destinationCountryCode": "USA",
                "productCategoryCode": "ELEC",
                "declaredValue": 1000.0
            }
            """;

        // When: POST request is made
        // Then: Should return 400 Bad Request
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/api/tariff/calculate")
        .then()
            .statusCode(400);
    }

    // ==================== Search Endpoint Tests ====================

    @Test
    void searchTariffs_ShouldReturnResults_WhenValidFiltersProvided() {
        // Given: Valid search parameters
        // When: GET request is made to /api/tariff/search with filters
        // Then: Should return 200 OK with array of results
        given()
            .queryParam("origin", "SGP")
            .queryParam("destination", "USA")
            .queryParam("category", "ELEC")
        .when()
            .get("/api/tariff/search")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(java.util.List.class));
    }

    @Test
    void searchTariffs_ShouldReturnAllResults_WhenNoFiltersProvided() {
        // Given: No search parameters
        // When: GET request is made without filters
        // Then: Should return 200 OK with all tariffs
        given()
        .when()
            .get("/api/tariff/search")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", isA(java.util.List.class));
    }

    // ==================== Authentication Tests ====================

    @Test
    void protectedEndpoint_ShouldReturn401_WhenNoAuthenticationProvided() {
        // Given: No authentication token
        // When: Request is made to a protected endpoint
        // Then: Should return 401 Unauthorized
        // Note: Update the endpoint if your protected endpoints are different
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/api/tariff/admin/all") // Assuming this is admin-only
        .then()
            .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    void protectedEndpoint_ShouldReturn200_WhenValidAuthenticationProvided() {
        // Given: Valid JWT token for admin user
        // When: Request is made with authentication
        // Then: Should return 200 OK
        given()
            .header("Authorization", "Bearer " + adminJwtToken)
        .when()
            .get("/api/tariff/admin/all")
        .then()
            .statusCode(200);
    }

    @Test
    void adminEndpoint_ShouldReturn403_WhenNonAdminUserAttempts() {
        // Given: Valid JWT token for regular user (not admin)
        // When: Request is made to admin-only endpoint
        // Then: Should return 403 Forbidden
        given()
            .header("Authorization", "Bearer " + userJwtToken)
        .when()
            .get("/api/tariff/admin/all")
        .then()
            .statusCode(403);
    }

    // ==================== CRUD Operations (Admin Only) ====================

    @Test
    void createTariff_ShouldReturn201_WhenAdminCreatesValidTariff() {
        // Given: Valid tariff creation request with admin token
        String newTariffJson = """
            {
                "originCountryCode": "SGP",
                "destinationCountryCode": "JPN",
                "productCategoryCode": "ELEC",
                "baseRate": 0.08,
                "additionalFee": 15.00,
                "effectiveFrom": "2025-01-01"
            }
            """;

        // When: POST request is made to create tariff
        // Then: Should return 201 Created
        given()
            .header("Authorization", "Bearer " + adminJwtToken)
            .contentType(ContentType.JSON)
            .body(newTariffJson)
        .when()
            .post("/api/tariff/admin")
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .contentType(ContentType.JSON)
            .body("originCountryCode", equalTo("SGP"))
            .body("destinationCountryCode", equalTo("JPN"));
    }

    @Test
    void createTariff_ShouldReturn403_WhenNonAdminAttempts() {
        // Given: Valid tariff creation request with regular user token
        String newTariffJson = """
            {
                "originCountryCode": "SGP",
                "destinationCountryCode": "JPN",
                "productCategoryCode": "ELEC",
                "baseRate": 0.08,
                "additionalFee": 15.00,
                "effectiveFrom": "2025-01-01"
            }
            """;

        // When: POST request is made by non-admin user
        // Then: Should return 403 Forbidden
        given()
            .header("Authorization", "Bearer " + userJwtToken)
            .contentType(ContentType.JSON)
            .body(newTariffJson)
        .when()
            .post("/api/tariff/admin")
        .then()
            .statusCode(403);
    }

    // ==================== CORS Tests ====================

    @Test
    void corsHeaders_ShouldBePresent_ForPreflightRequest() {
        // Given: A preflight OPTIONS request
        // When: OPTIONS request is made
        // Then: Should return appropriate CORS headers
        given()
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
        .when()
            .options("/api/tariff/calculate")
        .then()
            .statusCode(anyOf(is(200), is(204)))
            .header("Access-Control-Allow-Origin", notNullValue());
    }
}
