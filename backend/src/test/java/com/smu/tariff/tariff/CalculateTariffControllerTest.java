package com.smu.tariff.tariff;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.logging.QueryLogRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.repository.TariffRateRepository;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;
import com.smu.tariff.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for CalculateTariffController.
 * Tests the /api/calculate-tariff endpoint for tariff calculations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CalculateTariffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private TariffRateRepository tariffRateRepository;

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

    private Country singapore;
    private Country usa;
    private ProductCategory electronics;
    private ProductCategory textiles;
    private String token;

    @BeforeEach
    void setUp() {
        // GUARD: refuse to run if not in test profile
        if (!Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            throw new IllegalStateException(
                    "Refusing to run destructive test setup against non-test profile. " +
                            "Run with: -Dspring.profiles.active=test");
        }

        // Clean up in correct order
        tariffRateRepository.deleteAll();
        queryLogRepository.deleteAll();
        userRepository.deleteAll();
        productCategoryRepository.deleteAll();
        countryRepository.deleteAll();

        // Setup test data
        singapore = countryRepository.save(new Country("SGP", "Singapore"));
        usa = countryRepository.save(new Country("USA", "United States"));

        electronics = new ProductCategory();
        electronics.setCode("ELEC");
        electronics.setName("Electronics");
        electronics.setHsCode("85");
        electronics.setWeightBased(false);
        productCategoryRepository.save(electronics);

        textiles = new ProductCategory();
        textiles.setCode("TEXT");
        textiles.setName("Textiles");
        textiles.setHsCode("62");
        textiles.setWeightBased(true);
        productCategoryRepository.save(textiles);

        // Create test user
        User testUser = new User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("validPass123"),
                Role.USER);
        userRepository.save(testUser);
    // generate JWT for authenticated requests
    token = jwtService.generateToken(testUser);

        // Create tariff rates
        createTariffRate(singapore, usa, electronics, 5.0, 10.0);
        createTariffRate(singapore, usa, textiles, 3.0, 5.0);
    }

    private void createTariffRate(Country origin, Country dest, ProductCategory category,
                                   double baseRate, double additionalFee) {
        TariffRate rate = new TariffRate();
        rate.setOrigin(origin);
        rate.setDestination(dest);
        rate.setProductCategory(category);
        rate.setBaseRate(BigDecimal.valueOf(baseRate));
        rate.setAdditionalFee(BigDecimal.valueOf(additionalFee));
        rate.setWeightValue(0.0f);
        rate.setEffectiveFrom(LocalDate.now());
        rate.setEffectiveTo(LocalDate.now().plusYears(1));
        tariffRateRepository.save(rate);
    }

    // ==================== Success Tests ====================

    @Test
    void calculateTariff_ShouldReturnCalculation_WhenNonWeightBasedProduct() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "ELEC",
                    "declaredValue": 1000.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode", equalTo("ELEC")))
                .andExpect(jsonPath("$.hsCode", equalTo("85")))
                .andExpect(jsonPath("$.weightBased", equalTo(false)))
                .andExpect(jsonPath("$.calculatedTariff").isNumber())
                .andExpect(jsonPath("$.currency", equalTo("USD")));
    }

    @Test
    void calculateTariff_ShouldMultiplyByWeight_WhenWeightBasedProduct() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "TEXT",
                    "declaredValue": 100.0,
                    "weight": 10.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode", equalTo("TEXT")))
                .andExpect(jsonPath("$.weightBased", equalTo(true)))
                .andExpect(jsonPath("$.weight", equalTo(10.0)))
                .andExpect(jsonPath("$.calculatedTariff").isNumber());
    }

    @Test
    void calculateTariff_ShouldIgnoreWeight_WhenNonWeightBasedProduct() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "ELEC",
                    "declaredValue": 1000.0,
                    "weight": 99.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode", equalTo("ELEC")))
                .andExpect(jsonPath("$.weightBased", equalTo(false)))
                .andExpect(jsonPath("$.weight", equalTo(99.0)));
    }

    // ==================== Validation Error Tests ====================

    @Test
    void calculateTariff_ShouldReturn400_WhenDeclaredValueIsMissing() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "ELEC"
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("declaredValue")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenDeclaredValueIsZero() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "ELEC",
                    "declaredValue": 0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("declaredValue")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenDeclaredValueIsNegative() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "ELEC",
                    "declaredValue": -100
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("declaredValue")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenWeightMissingForWeightBasedProduct() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "TEXT",
                    "declaredValue": 1000.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Weight")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenWeightIsNegativeForWeightBasedProduct() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "TEXT",
                    "declaredValue": 1000.0,
                    "weight": -5.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Weight")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenWeightExceedsMaximum() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "TEXT",
                    "declaredValue": 1000.0,
                    "weight": 20000.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Weight")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenProductCodeIsInvalid() throws Exception {
        String request = """
                {
                    "originCountry": "SGP",
                    "destCountry": "USA",
                    "productCode": "INVALID",
                    "declaredValue": 1000.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Unknown product code")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenOriginCountryIsInvalid() throws Exception {
        String request = """
                {
                    "originCountry": "XXX",
                    "destCountry": "USA",
                    "productCode": "ELEC",
                    "declaredValue": 1000.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("country")));
    }

    @Test
    void calculateTariff_ShouldReturn400_WhenRequiredFieldsAreMissing() throws Exception {
        String request = """
                {
                    "declaredValue": 1000.0
                }
                """;

    mockMvc.perform(post("/api/calculate-tariff")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
                .andExpect(status().isBadRequest());
    }
}
