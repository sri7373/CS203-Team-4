package com.smu.tariff.trade;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.TariffRateRepository;
import com.smu.tariff.user.Role;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;
import com.smu.tariff.logging.QueryLogRepository;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for TradeAnalyticsController.
 * Tests the /api/trade/insights endpoint for generating country trade insights.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TradeAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private TariffRateRepository tariffRateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueryLogRepository queryLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    private Country singapore;
    private Country usa;
    private Country japan;
    private ProductCategory electronics;
    private ProductCategory food;

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
        countryRepository.deleteAll();

        // Setup test data
        singapore = countryRepository.save(new Country("SGP", "Singapore"));
        usa = countryRepository.save(new Country("USA", "United States"));
        japan = countryRepository.save(new Country("JPN", "Japan"));

        electronics = new ProductCategory();
        electronics.setCode("ELEC");
        electronics.setName("Electronics");
        electronics.setHsCode("85");
        electronics.setWeightBased(false);

        food = new ProductCategory();
        food.setCode("FOOD");
        food.setName("Food & Beverages");
        food.setHsCode("04");
        food.setWeightBased(false);

        // Create test user
        User testUser = new User(
                "testuser",
                "test@example.com",
                passwordEncoder.encode("password123"),
                Role.USER);
        userRepository.save(testUser);

        // Create some tariff rates for testing
        createTariffRate(singapore, usa, electronics, 5.0, 10.0);
        createTariffRate(singapore, japan, food, 3.0, 5.0);
        createTariffRate(usa, singapore, electronics, 7.0, 15.0);
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
    void getInsights_ShouldReturnTradeData_WhenCountryExists() throws Exception {
        mockMvc.perform(get("/api/trade/insights")
                .param("country", "SGP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode", equalTo("SGP")))
                .andExpect(jsonPath("$.countryName", equalTo("Singapore")))
                .andExpect(jsonPath("$.topImports").isArray())
                .andExpect(jsonPath("$.topExports").isArray())
                .andExpect(jsonPath("$.majorPartners").isArray())
                .andExpect(jsonPath("$.averageImportTariff").exists())
                .andExpect(jsonPath("$.averageExportTariff").exists());
    }

    @Test
    void getInsights_ShouldBeCaseInsensitive_WhenCountryCodeProvided() throws Exception {
        mockMvc.perform(get("/api/trade/insights")
                .param("country", "sgp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode", equalTo("SGP")));
    }

    @Test
    void getInsights_ShouldHandleCountryWithNoData_WhenCountryHasNoTariffs() throws Exception {
        // Japan has only one incoming tariff, limited data
        mockMvc.perform(get("/api/trade/insights")
                .param("country", "JPN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode", equalTo("JPN")))
                .andExpect(jsonPath("$.countryName", equalTo("Japan")));
    }

    // ==================== Error Tests ====================

    @Test
    void getInsights_ShouldReturn400_WhenCountryCodeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/trade/insights")
                .param("country", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Unknown country code")));
    }

    @Test
    void getInsights_ShouldReturn400_WhenCountryParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/trade/insights"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getInsights_ShouldReturn400_WhenCountryCodeIsEmpty() throws Exception {
        mockMvc.perform(get("/api/trade/insights")
                .param("country", ""))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Country code is required")));
    }

    @Test
    void getInsights_ShouldReturn400_WhenCountryCodeIsWhitespace() throws Exception {
        mockMvc.perform(get("/api/trade/insights")
                .param("country", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Country code is required")));
    }
}
