package com.smu.tariff.reference;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.logging.QueryLogRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.security.JwtService;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for ReferenceController.
 * Tests the /api/reference/* endpoints for retrieving reference data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueryLogRepository queryLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Environment environment;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // GUARD: refuse to run if not in test profile
        if (!Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            throw new IllegalStateException(
                    "Refusing to run destructive test setup against non-test profile. " +
                            "Run with: -Dspring.profiles.active=test");
        }

        // Clean up in correct order
        queryLogRepository.deleteAll();
        userRepository.deleteAll();
        productCategoryRepository.deleteAll();
        countryRepository.deleteAll();

        // Setup test data
        countryRepository.save(new Country("SGP", "Singapore"));
        countryRepository.save(new Country("USA", "United States"));
        countryRepository.save(new Country("CHN", "China"));

                        productCategoryRepository.findByCode("ELEC")
                                .orElseGet(() -> {
                                        ProductCategory cat = new ProductCategory();
                                        cat.setCode("ELEC");
                                        cat.setName("Electronics");
                                        cat.setHsCode("85");
                                        cat.setWeightBased(false);
                                        return productCategoryRepository.save(cat);
                                });

                        productCategoryRepository.findByCode("STEEL")
                                .orElseGet(() -> {
                                        ProductCategory cat = new ProductCategory();
                                        cat.setCode("STEEL");
                                        cat.setName("Steel Products");
                                        cat.setHsCode("72");
                                        cat.setWeightBased(false);
                                        return productCategoryRepository.save(cat);
                                });

        // Create test users with different roles
        User regularUser = new User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("validPass123"),
                Role.USER);
        userRepository.save(regularUser);

        User adminUser = new User(
                "adminuser",
                "admin@example.com",
                passwordEncoder.encode("validPass123"),
                Role.ADMIN);
        userRepository.save(adminUser);

        // Generate JWT tokens
        userToken = jwtService.generateToken(regularUser);
        adminToken = jwtService.generateToken(adminUser);
    }

    // ==================== Countries Endpoint Tests ====================

    @Test
    void listCountries_ShouldReturnCountries_WhenUserAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reference/countries")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[?(@.code == 'SGP')].name", hasItem("Singapore")))
                .andExpect(jsonPath("$[?(@.code == 'USA')].name", hasItem("United States")))
                .andExpect(jsonPath("$[?(@.code == 'CHN')].name", hasItem("China")));
    }

    @Test
    void listCountries_ShouldReturnCountries_WhenAdminAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reference/countries")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void listCountries_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reference/countries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCountries_ShouldReturn401_WhenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/reference/countries")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCountries_ShouldUseFallbackNames_WhenCountryNotInDatabase() throws Exception {
        // MYS and IDN are in the hardcoded list but not in our test database
        mockMvc.perform(get("/api/reference/countries")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'MYS')].name", hasItem("Malaysia")))
                .andExpect(jsonPath("$[?(@.code == 'IDN')].name", hasItem("Indonesia")));
    }

    // ==================== Product Categories Endpoint Tests ====================

    @Test
    void listProductCategories_ShouldReturnCategories_WhenUserAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reference/product-categories")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[?(@.code == 'ELEC')].name", hasItem("Electronics")))
                .andExpect(jsonPath("$[?(@.code == 'STEEL')].name", hasItem("Steel Products")));
    }

    @Test
    void listProductCategories_ShouldReturnCategories_WhenAdminAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reference/product-categories")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void listProductCategories_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/reference/product-categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listProductCategories_ShouldReturn401_WhenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/reference/product-categories")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listProductCategories_ShouldUseFallbackNames_WhenCategoryNotInDatabase() throws Exception {
        // FOOD is in the hardcoded list but not in our test database
        mockMvc.perform(get("/api/reference/product-categories")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'FOOD')].name", hasItem("Food Commodities")));
    }
}
