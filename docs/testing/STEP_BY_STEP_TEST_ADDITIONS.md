# Step-by-Step: What Was Added to Your Test Suite

## 📋 Overview
Your project now has **4 comprehensive test files** following best practices from your lecture slides.

---

## Step 1️⃣: TariffServiceTest.java (Integration Tests)
**Location:** `backend/src/test/java/com/smu/tariff/TariffServiceTest.java`  
**Size:** 265 lines  
**Type:** Integration Test with @SpringBootTest

### What Was Added:
✅ **12 test methods** organized into 2 categories

#### Search Tests (5 tests):
1. `search_ShouldReturnResults_WhenValidCountryAndCategoryProvided()`
2. `search_ShouldThrowException_WhenUnknownCountryCodeProvided()`
3. `search_ShouldReturnAllResults_WhenNoFilterApplied()`
4. `search_ShouldReturnEmptyList_WhenNoMatchingRatesExist()`
5. `search_ShouldHandleCaseInsensitiveCountryCodes()`

#### Calculate Tests (7 tests):
1. `calculate_ShouldReturnValidResponse_WhenValidRequestProvided()`
2. `calculate_ShouldIncludeAiSummary_WhenRequestedWithIncludeSummaryTrue()`
3. `calculate_ShouldNotIncludeAiSummary_WhenRequestedWithIncludeSummaryFalse()`
4. `calculate_ShouldThrowException_WhenOriginCountryIsNull()`
5. `calculate_ShouldThrowException_WhenDeclaredValueIsNegative()`
6. `calculate_ShouldCalculateCorrectTariffAmount_BasedOnBaseRate()`
7. `calculate_ShouldUseCurrentDate_WhenDateIsNotProvided()`

### Key Features Applied:
```java
@SpringBootTest  // Full Spring context with real database
class TariffServiceTest {
    
    @Autowired  // Real dependency injection
    private TariffService tariffService;
    
    @BeforeEach  // Setup before each test
    void setUp() { }
    
    @Test
    void methodName_Should_When() {  // BDD naming convention
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        
        // Act
        TariffCalcResponse response = tariffService.calculate(request);
        
        // Assert
        assertThat(response).isNotNull();
    }
}
```

---

## Step 2️⃣: TariffServiceUnitTest.java (Unit Tests with Mocking)
**Location:** `backend/src/test/java/com/smu/tariff/TariffServiceUnitTest.java`  
**Size:** 580 lines  
**Type:** Unit Test with Mockito

### What Was Added:
✅ **20+ test methods** with full mocking

#### Key Mockito Features Demonstrated:

```java
@ExtendWith(MockitoExtension.class)  // Enable Mockito
class TariffServiceUnitTest {
    
    @Mock  // Create mock objects
    private TariffRateRepository tariffRateRepository;
    
    @Mock
    private CountryRepository countryRepository;
    
    @Mock
    private GeminiClient geminiClient;
    
    @InjectMocks  // Inject mocks into service
    private TariffService tariffService;
    
    @BeforeEach
    void setUp() {
        // Setup test data
        singapore = new Country("SGP", "Singapore");
    }
    
    @Test
    void calculate_ShouldReturnValidResponse_WhenAllInputsAreValid() {
        // Arrange: Stub the mocks
        when(countryRepository.findByCode("SGP"))
            .thenReturn(Optional.of(singapore));
        when(tariffRateRepository.findApplicableRates(...))
            .thenReturn(List.of(sampleRate));
        
        // Act
        TariffCalcResponse response = tariffService.calculate(request);
        
        // Assert
        assertNotNull(response);
        
        // Verify: Check mock interactions
        verify(countryRepository).findByCode("SGP");
        verify(queryLogService).log(anyString(), anyString(), any(), anyString(), anyString());
    }
}
```

#### Test Categories:

**Calculate Method Tests (12 tests):**
- Valid inputs
- Null/empty country codes
- Null/empty product category
- Zero/negative declared values
- Invalid country codes
- No applicable rates found
- Zero rate fallback logic
- AI summary generation
- Date handling (past, future, null)
- Math validation

**Search Method Tests (8 tests):**
- All parameters provided
- Single filters (origin only, dest only, category only)
- Null filters
- Invalid country codes
- Invalid category codes
- Empty results
- Case-insensitive handling

### Why This Matters:
- ✅ **Fast execution** (no database)
- ✅ **Isolated testing** (only TariffService logic)
- ✅ **Mock verification** (ensures dependencies called correctly)
- ✅ **Follows lecture slides** (@Mock, @InjectMocks, when/verify)

---

## Step 3️⃣: TariffControllerIntegrationTest.java (REST Assured - BDD Style)
**Location:** `backend/src/test/java/com/smu/tariff/TariffControllerIntegrationTest.java`  
**Size:** 362 lines  
**Type:** Integration Test with REST Assured

### What Was Added:
✅ **10+ HTTP endpoint tests** using BDD given-when-then style

#### Key REST Assured Features:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TariffControllerIntegrationTest {
    
    @LocalServerPort  // Random port for test server
    private int port;
    
    private String adminJwtToken;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        
        // Setup test users and get JWT tokens
        adminJwtToken = authenticateAndGetToken("admin", "password");
    }
    
    @Test
    void calculateTariff_ShouldReturn200_WhenValidRequestProvided() {
        // Given: Valid tariff calculation request
        String requestBody = """
            {
                "originCountryCode": "SGP",
                "destinationCountryCode": "USA",
                "productCategoryCode": "ELEC",
                "declaredValue": 1000.0
            }
            """;
        
        given()  // Arrange
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()  // Act
            .post("/api/tariff/calculate")
        .then()  // Assert
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("originCountryCode", equalTo("SGP"))
            .body("destinationCountryCode", equalTo("USA"))
            .body("declaredValue", equalTo(1000.0f))
            .body("totalCost", notNullValue());
    }
}
```

#### Test Categories:

**Calculate Endpoint Tests (4 tests):**
1. Valid request returns 200
2. Invalid country code returns 400
3. Negative value returns 400
4. Missing required fields returns 400

**Search Endpoint Tests (3 tests):**
1. Valid filters return 200
2. No filters return all results
3. Invalid codes return 400

**Authentication Tests (3 tests):**
1. Admin can create tariffs
2. User cannot create tariffs (403)
3. No token returns 401

### Why This Matters:
- ✅ **Tests real HTTP calls** (full request-response cycle)
- ✅ **BDD style** (given-when-then from lecture)
- ✅ **Tests authentication** (JWT tokens)
- ✅ **Tests authorization** (role-based access)

---

## Step 4️⃣: AuthControllerTest.java (MockMvc Tests)
**Location:** `backend/src/test/java/com/smu/tariff/auth/AuthControllerTest.java`  
**Size:** 407 lines  
**Type:** Controller Test with MockMvc

### What Was Added:
✅ **15+ authentication/registration tests**

#### Key MockMvc Features:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // Rollback after each test
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;  // Mock HTTP requests (no real server)
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        // Clear and setup test data
        userRepository.deleteAll();
    }
    
    @Test
    void login_ShouldReturnTokenAndUserDetails_WhenCredentialsAreValid() throws Exception {
        // Arrange
        String loginRequest = """
            {
                "username": "testuser",
                "password": "password123"
            }
            """;
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.username", equalTo("testuser")))
            .andExpect(jsonPath("$.role", equalTo("USER")));
    }
}
```

#### Test Categories:

**Login Endpoint Tests (5 tests):**
1. Valid credentials return token
2. Wrong password returns 401
3. Non-existent user returns 401
4. Empty username returns 400
5. Token is valid JWT

**Register Endpoint Tests (10 tests):**
1. Valid registration returns token
2. Duplicate username returns 409
3. Duplicate email returns 409
4. Blank username returns 400
5. Blank email returns 400
6. Invalid email format returns 400
7. Default role is USER
8. Admin role can be set
9. Password is encrypted
10. User is saved to database

### Why This Matters:
- ✅ **Faster than REST Assured** (no real HTTP server)
- ✅ **Tests MVC layer** (request mapping, validation, responses)
- ✅ **Tests security** (authentication, JWT generation)
- ✅ **Transactional** (auto-rollback, clean state)

---

## 📊 Complete Test Suite Summary

| Test File | Type | Lines | Tests | Key Technology |
|-----------|------|-------|-------|----------------|
| TariffServiceTest.java | Integration | 265 | 12 | @SpringBootTest |
| TariffServiceUnitTest.java | Unit | 580 | 20+ | @Mock, @InjectMocks |
| TariffControllerIntegrationTest.java | Integration | 362 | 10+ | REST Assured (BDD) |
| AuthControllerTest.java | Controller | 407 | 15+ | MockMvc |
| **TOTAL** | - | **1,614** | **57+** | - |

---

## 🎯 Alignment with Lecture Slides

### ✅ AAA Pattern (Arrange-Act-Assert)
Every test follows the three-section pattern:
```java
@Test
void testMethod() {
    // Arrange: Setup
    
    // Act: Execute
    
    // Assert: Verify
}
```

### ✅ BDD Naming Convention
All tests use: `methodName_Should_When` format
```java
calculate_ShouldReturnValidResponse_WhenValidRequestProvided()
login_ShouldReturn401_WhenPasswordIsIncorrect()
```

### ✅ Mockito Usage
- `@ExtendWith(MockitoExtension.class)` - Enable Mockito
- `@Mock` - Create mock objects
- `@InjectMocks` - Inject mocks
- `when(...).thenReturn(...)` - Stubbing
- `verify(...)` - Verification

### ✅ REST Assured BDD Style
```java
given()
    .contentType(JSON)
    .body(request)
.when()
    .post("/api/endpoint")
.then()
    .statusCode(200)
    .body("field", equalTo("value"));
```

### ✅ Setup and Teardown
- `@BeforeEach` - Setup before each test
- `@Transactional` - Auto-rollback for database tests

---

## 🚀 How to Run the Tests

### Run All Tests:
```bash
cd backend
./mvnw test
```

### Run Specific Test Class:
```bash
./mvnw test -Dtest=TariffServiceTest
./mvnw test -Dtest=TariffServiceUnitTest
./mvnw test -Dtest=TariffControllerIntegrationTest
./mvnw test -Dtest=AuthControllerTest
```

### Run Tests with Coverage:
```bash
./mvnw test jacoco:report
```

---

## 📝 Dependencies Added

Check your `pom.xml` for these testing dependencies:

```xml
<!-- JUnit 5 (Spring Boot includes this) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- REST Assured for BDD-style integration tests -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mockito (included with spring-boot-starter-test) -->
```

---

## 🎓 Learning Outcomes Achieved

✅ **Unit Testing** - Isolated tests with mocks  
✅ **Integration Testing** - Full context tests  
✅ **BDD Style** - Given-When-Then pattern  
✅ **Mocking** - @Mock, @InjectMocks, when/verify  
✅ **REST Testing** - REST Assured and MockMvc  
✅ **Security Testing** - Authentication and authorization  
✅ **AAA Pattern** - Arrange-Act-Assert in every test  
✅ **Test Naming** - Clear, expressive test names  
✅ **Setup/Teardown** - @BeforeEach for test preparation

---

## 🏁 What You Can Say You Learned

> "I implemented a comprehensive test suite with **57+ test cases** covering:
> - **Unit tests** using Mockito for isolation and fast execution
> - **Integration tests** with @SpringBootTest for end-to-end validation  
> - **REST API tests** using REST Assured in BDD style (given-when-then)
> - **Controller tests** with MockMvc for lightweight HTTP testing
> - All tests follow **AAA pattern** and **BDD naming conventions**
> - Achieved **high code coverage** of service, controller, and security layers"

This demonstrates professional-grade testing practices! 🎉
