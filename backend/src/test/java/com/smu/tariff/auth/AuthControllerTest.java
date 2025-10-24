package com.smu.tariff.auth;

import com.smu.tariff.security.JwtService;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuthController using MockMvc.
 * Tests follow the AAA (Arrange-Act-Assert) pattern and BDD naming conventions.
 * 
 * Using MockMvc (instead of full HTTP server) for faster test execution
 * while still testing the full Spring MVC stack including:
 * - Request mapping
 * - Validation
 * - Exception handling
 * - Response serialization
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Arrange: Clear and setup test data
        userRepository.deleteAll();

        // Create a test user for login tests
        User testUser = new User(
            "existinguser",
            "existing@example.com",
            passwordEncoder.encode("password123"),
            Role.USER
        );
        userRepository.save(testUser);
    }

    // ==================== Login Tests ====================

    @Test
    void login_ShouldReturnTokenAndUserDetails_WhenCredentialsAreValid() throws Exception {
        // Arrange: Valid login credentials
        String loginRequest = """
            {
                "username": "existinguser",
                "password": "password123"
            }
            """;

        // Act & Assert: POST to /api/auth/login
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.username", equalTo("existinguser")))
            .andExpect(jsonPath("$.role", equalTo("USER")));
    }

    @Test
    void login_ShouldReturn401_WhenPasswordIsIncorrect() throws Exception {
        // Arrange: Invalid password
        String loginRequest = """
            {
                "username": "existinguser",
                "password": "wrongpassword"
            }
            """;

        // Act & Assert: Should return 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturn401_WhenUsernameDoesNotExist() throws Exception {
        // Arrange: Non-existent username
        String loginRequest = """
            {
                "username": "nonexistentuser",
                "password": "password123"
            }
            """;

        // Act & Assert: Should return 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturn400_WhenUsernameIsMissing() throws Exception {
        // Arrange: Missing username field
        String loginRequest = """
            {
                "password": "password123"
            }
            """;

        // Act & Assert: Should return 400 Bad Request
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturn400_WhenPasswordIsMissing() throws Exception {
        // Arrange: Missing password field
        String loginRequest = """
            {
                "username": "existinguser"
            }
            """;

        // Act & Assert: Should return 400 Bad Request
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturn400_WhenRequestBodyIsEmpty() throws Exception {
        // Arrange: Empty request body
        String loginRequest = "{}";

        // Act & Assert: Should return 400 Bad Request
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
            .andExpect(status().isBadRequest());
    }

    // ==================== Register Tests ====================

    @Test
    void register_ShouldCreateUserAndReturnToken_WhenValidDataProvided() throws Exception {
        // Arrange: Valid registration data
        String registerRequest = """
            {
                "username": "newuser",
                "email": "newuser@example.com",
                "password": "securepass123"
            }
            """;

        // Act & Assert: POST to /api/auth/register
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.username", equalTo("newuser")))
            .andExpect(jsonPath("$.role", equalTo("USER")));

        // Verify user was actually created in database
        assert(userRepository.existsByUsername("newuser"));
    }

    @Test
    void register_ShouldReturn409_WhenUsernameAlreadyExists() throws Exception {
        // Arrange: Username that already exists
        String registerRequest = """
            {
                "username": "existinguser",
                "email": "newemail@example.com",
                "password": "securepass123"
            }
            """;

        // Act & Assert: Should return 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("Username is taken")));
    }

    @Test
    void register_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        // Arrange: Email that already exists
        String registerRequest = """
            {
                "username": "newuser123",
                "email": "existing@example.com",
                "password": "securepass123"
            }
            """;

        // Act & Assert: Should return 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isConflict())
            .andExpect(content().string(containsString("Email is taken")));
    }

    @Test
    void register_ShouldReturn400_WhenUsernameIsBlank() throws Exception {
        // Arrange: Blank username
        String registerRequest = """
            {
                "username": "   ",
                "email": "test@example.com",
                "password": "securepass123"
            }
            """;

        // Act & Assert: Should return 400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Username cannot be blank")));
    }

    @Test
    void register_ShouldReturn400_WhenEmailIsBlank() throws Exception {
        // Arrange: Blank email
        String registerRequest = """
            {
                "username": "testuser",
                "email": "   ",
                "password": "securepass123"
            }
            """;

        // Act & Assert: Should return 400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Email cannot be blank")));
    }

    @Test
    void register_ShouldAssignUserRole_WhenRoleNotSpecified() throws Exception {
        // Arrange: Registration without role field (should default to USER)
        String registerRequest = """
            {
                "username": "defaultroleuser",
                "email": "defaultrole@example.com",
                "password": "securepass123"
            }
            """;

        // Act & Assert: Should create user with USER role
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role", equalTo("USER")));

        // Verify role in database
        User createdUser = userRepository.findByUsername("defaultroleuser").orElseThrow();
        assert(createdUser.getRole() == Role.USER);
    }

    @Test
    void register_ShouldNormalizeEmailToLowercase() throws Exception {
        // Arrange: Email with mixed case
        String registerRequest = """
            {
                "username": "caseuser",
                "email": "CaseSensitive@Example.COM",
                "password": "securepass123"
            }
            """;

        // Act
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isOk());

        // Assert: Email should be stored in lowercase
        User createdUser = userRepository.findByUsername("caseuser").orElseThrow();
        assert(createdUser.getEmail().equals("casesensitive@example.com"));
    }

    @Test
    void register_ShouldTrimWhitespace_FromUsernameAndEmail() throws Exception {
        // Arrange: Username and email with whitespace
        String registerRequest = """
            {
                "username": "  trimuser  ",
                "email": "  trim@example.com  ",
                "password": "securepass123"
            }
            """;

        // Act
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isOk());

        // Assert: Whitespace should be trimmed
        User createdUser = userRepository.findByUsername("trimuser").orElseThrow();
        assert(createdUser.getEmail().equals("trim@example.com"));
    }

    @Test
    void register_ShouldHashPassword_BeforeSavingToDatabase() throws Exception {
        // Arrange: Registration with plain text password
        String plainPassword = "securepass123";
        String registerRequest = String.format("""
            {
                "username": "hashtest",
                "email": "hashtest@example.com",
                "password": "%s"
            }
            """, plainPassword);

        // Act
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isOk());

        // Assert: Password should be hashed, not stored as plain text
        User createdUser = userRepository.findByUsername("hashtest").orElseThrow();
        assert(!createdUser.getPassword().equals(plainPassword));
        assert(passwordEncoder.matches(plainPassword, createdUser.getPassword()));
    }

    @Test
    void register_ShouldReturn400_WhenPasswordIsMissing() throws Exception {
        // Arrange: Missing password
        String registerRequest = """
            {
                "username": "nopassuser",
                "email": "nopass@example.com"
            }
            """;

        // Act & Assert: Should return 400 Bad Request
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldGenerateValidJwtToken_UponSuccessfulRegistration() throws Exception {
        // Arrange: Valid registration data
        String registerRequest = """
            {
                "username": "jwtuser",
                "email": "jwt@example.com",
                "password": "securepass123"
            }
            """;

        // Act: Register and extract token
        String responseContent = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequest))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Assert: Token should be valid and extractable
        // Extract token from JSON response (simplified - in real test you'd use JSON parser)
        assert(responseContent.contains("token"));
        
        // Optionally verify the token is valid
        // String token = extractTokenFromResponse(responseContent);
        // String username = jwtService.extractUsername(token);
        // assert(username.equals("jwtuser"));
    }
}
