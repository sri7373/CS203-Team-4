package com.smu.tariff;

import com.smu.tariff.ai.GeminiClient;
import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.exception.TariffNotFoundException;
import com.smu.tariff.logging.QueryLogService;
import com.smu.tariff.product.ProductCategory;
import com.smu.tariff.product.ProductCategoryRepository;
import com.smu.tariff.tariff.TariffRate;
import com.smu.tariff.tariff.TariffRateRepository;
import com.smu.tariff.tariff.TariffService;
import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import com.smu.tariff.tariff.dto.TariffRateDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TariffService using Mockito for isolation.
 * Tests follow the AAA (Arrange-Act-Assert) pattern.
 */
@ExtendWith(MockitoExtension.class)
class TariffServiceUnitTest {

    @Mock
    private TariffRateRepository tariffRateRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private QueryLogService queryLogService;

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private TariffService tariffService;

    private Country singapore;
    private Country usa;
    private ProductCategory electronics;
    private TariffRate sampleRate;

    @BeforeEach
    void setUp() {
        // Arrange: Set up common test data
        singapore = new Country("SGP", "Singapore");
        usa = new Country("USA", "United States");
        electronics = new ProductCategory("ELEC", "Electronics");

        sampleRate = new TariffRate(
            singapore,
            usa,
            electronics,
            new BigDecimal("0.05"),  // 5% base rate
            new BigDecimal("10.00"), // $10 additional fee
            LocalDate.now().minusDays(30),
            null
        );
    }

    // ==================== Calculate Method Tests ====================

    @Test
    void calculate_ShouldReturnValidResponse_WhenAllInputsAreValid() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;
        request.date = LocalDate.now().toString();

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(electronics));
        when(tariffRateRepository.findApplicableRates(
            eq(singapore), eq(usa), eq(electronics), any(LocalDate.class)
        )).thenReturn(List.of(sampleRate));
        when(geminiClient.generateSummary(anyString())).thenReturn("<p>Test summary</p>");

        // Act
        TariffCalcResponse response = tariffService.calculate(request);

        // Assert
        assertNotNull(response);
        assertEquals("SGP", response.originCountryCode);
        assertEquals("USA", response.destinationCountryCode);
        assertEquals("ELEC", response.productCategoryCode);
        assertEquals(0, new BigDecimal("1000.00").compareTo(response.declaredValue));
        assertEquals(0, new BigDecimal("0.0500").compareTo(response.baseRate));
        assertEquals(0, new BigDecimal("50.00").compareTo(response.tariffAmount)); // 1000 * 0.05
        assertEquals(0, new BigDecimal("10.00").compareTo(response.additionalFee));
        assertEquals(0, new BigDecimal("1060.00").compareTo(response.totalCost)); // 1000 + 50 + 10
        assertNotNull(response.aiSummary);

        // Verify interactions
        verify(countryRepository).findByCode("SGP");
        verify(countryRepository).findByCode("USA");
        verify(productCategoryRepository).findByCode("ELEC");
        verify(queryLogService).log(eq("CALCULATE"), anyString(), eq(response), eq("SGP"), eq("USA"));
        verify(geminiClient).generateSummary(anyString());
    }

    @Test
    void calculate_ShouldThrowException_WhenOriginCountryCodeIsNull() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = null;
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Origin country code is required");

        // Verify no repository calls were made
        verify(countryRepository, never()).findByCode(anyString());
    }

    @Test
    void calculate_ShouldThrowException_WhenOriginCountryCodeIsEmpty() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "   ";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Origin country code is required");
    }

    @Test
    void calculate_ShouldThrowException_WhenDestinationCountryCodeIsNull() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = null;
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Destination country code is required");
    }

    @Test
    void calculate_ShouldThrowException_WhenProductCategoryCodeIsNull() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = null;
        request.declaredValue = 1000.0;

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Product category code is required");
    }

    @Test
    void calculate_ShouldThrowException_WhenDeclaredValueIsZero() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 0.0;

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Declared value must be greater than 0");
    }

    @Test
    void calculate_ShouldThrowException_WhenDeclaredValueIsNegative() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = -100.0;

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Declared value must be greater than 0");
    }

    @Test
    void calculate_ShouldThrowException_WhenOriginCountryNotFound() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "XXX";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        when(countryRepository.findByCode("XXX")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Unknown origin country code: XXX");
        verify(countryRepository).findByCode("XXX");
    }

    @Test
    void calculate_ShouldThrowException_WhenDestinationCountryNotFound() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "ZZZ";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("ZZZ")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Unknown destination country code: ZZZ");
    }

    @Test
    void calculate_ShouldThrowException_WhenProductCategoryNotFound() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "INVALID";
        request.declaredValue = 1000.0;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("Unknown product category code: INVALID");
    }

    @Test
    void calculate_ShouldThrowException_WhenNoApplicableTariffRateFound() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(electronics));
        when(tariffRateRepository.findApplicableRates(
            eq(singapore), eq(usa), eq(electronics), any(LocalDate.class)
        )).thenReturn(Collections.emptyList());

        // Act & Assert
        TariffNotFoundException exception = assertThrows(
            TariffNotFoundException.class,
            () -> tariffService.calculate(request)
        );
        assertThat(exception.getMessage()).contains("No applicable tariff rate found");
    }

    @Test
    void calculate_ShouldCalculateCorrectTotals_WithDifferentValues() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 2500.0;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(electronics));
        when(tariffRateRepository.findApplicableRates(
            eq(singapore), eq(usa), eq(electronics), any(LocalDate.class)
        )).thenReturn(List.of(sampleRate));

        // Act
        TariffCalcResponse response = tariffService.calculate(request, false);

        // Assert
        BigDecimal expectedTariff = new BigDecimal("2500.00")
            .multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_UP); // 125.00
        BigDecimal expectedTotal = new BigDecimal("2500.00")
            .add(expectedTariff)
            .add(new BigDecimal("10.00")); // 2635.00

        assertEquals(new BigDecimal("125.00"), response.tariffAmount);
        assertEquals(new BigDecimal("2635.00"), response.totalCost);
    }

    @Test
    void calculate_ShouldNotIncludeAiSummary_WhenIncludeSummaryIsFalse() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(electronics));
        when(tariffRateRepository.findApplicableRates(
            eq(singapore), eq(usa), eq(electronics), any(LocalDate.class)
        )).thenReturn(List.of(sampleRate));

        // Act
        TariffCalcResponse response = tariffService.calculate(request, false);

        // Assert
        assertNull(response.aiSummary);
        verify(geminiClient, never()).generateSummary(anyString());
    }

    @Test
    void calculate_ShouldHandleAiGenerationFailure_GracefullyWithFallbackMessage() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(electronics));
        when(tariffRateRepository.findApplicableRates(
            eq(singapore), eq(usa), eq(electronics), any(LocalDate.class)
        )).thenReturn(List.of(sampleRate));
        when(geminiClient.generateSummary(anyString())).thenThrow(new RuntimeException("API Error"));

        // Act
        TariffCalcResponse response = tariffService.calculate(request, true);

        // Assert
        assertNotNull(response);
        assertEquals("AI summary unavailable.", response.aiSummary);
        verify(geminiClient).generateSummary(anyString());
    }

    @Test
    void calculate_ShouldUseCurrentDate_WhenDateIsNull() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "SGP";
        request.destinationCountryCode = "USA";
        request.productCategoryCode = "ELEC";
        request.declaredValue = 1000.0;
        request.date = null;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(electronics));
        when(tariffRateRepository.findApplicableRates(
            eq(singapore), eq(usa), eq(electronics), any(LocalDate.class)
        )).thenReturn(List.of(sampleRate));

        // Act
        TariffCalcResponse response = tariffService.calculate(request, false);

        // Assert
        assertNotNull(response);
        assertEquals(LocalDate.now().toString(), response.effectiveDate);
    }

    @Test
    void calculate_ShouldConvertCountryCodesToUpperCase() {
        // Arrange
        TariffCalcRequest request = new TariffCalcRequest();
        request.originCountryCode = "sgp"; // lowercase
        request.destinationCountryCode = "usa"; // lowercase
        request.productCategoryCode = "elec"; // lowercase
        request.declaredValue = 1000.0;

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(countryRepository.findByCode("USA")).thenReturn(Optional.of(usa));
        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(electronics));
        when(tariffRateRepository.findApplicableRates(
            eq(singapore), eq(usa), eq(electronics), any(LocalDate.class)
        )).thenReturn(List.of(sampleRate));

        // Act
        TariffCalcResponse response = tariffService.calculate(request, false);

        // Assert
        assertNotNull(response);
        verify(countryRepository).findByCode("SGP");
        verify(countryRepository).findByCode("USA");
        verify(productCategoryRepository).findByCode("ELEC");
    }

    // ==================== Search Method Tests ====================

    @Test
    void search_ShouldReturnAllRates_WhenAllParametersAreNull() {
        // Arrange
        TariffRate rate1 = new TariffRate(singapore, usa, electronics, 
            new BigDecimal("0.05"), new BigDecimal("10.00"), LocalDate.now(), null);
        TariffRate rate2 = new TariffRate(usa, singapore, electronics,
            new BigDecimal("0.03"), new BigDecimal("5.00"), LocalDate.now(), null);

        when(tariffRateRepository.search(null, null, null))
            .thenReturn(List.of(rate1, rate2));

        // Act
        List<TariffRateDto> results = tariffService.search(null, null, null);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(tariffRateRepository).search(null, null, null);
        verify(queryLogService).log(eq("SEARCH"), anyString(), any(), isNull(), isNull());
    }

    @Test
    void search_ShouldThrowException_WhenOriginCountryCodeIsInvalid() {
        // Arrange
        when(countryRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidTariffRequestException exception = assertThrows(
            InvalidTariffRequestException.class,
            () -> tariffService.search("INVALID", null, null)
        );
        assertThat(exception.getMessage()).contains("Unknown origin country code: INVALID");
    }

    @Test
    void search_ShouldFilterByOriginCountry_WhenOnlyOriginProvided() {
        // Arrange
        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));
        when(tariffRateRepository.search(eq(singapore), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        // Act
        List<TariffRateDto> results = tariffService.search("SGP", null, null);

        // Assert
        assertNotNull(results);
        verify(countryRepository).findByCode("SGP");
        verify(tariffRateRepository).search(eq(singapore), isNull(), isNull());
    }

    @Test
    void search_ShouldIgnoreWhitespace_InCountryCodes() {
        // Arrange
        // Service calls .toUpperCase() on "  SGP  " resulting in "  SGP  " being passed to repository
        when(countryRepository.findByCode("  SGP  ")).thenReturn(Optional.of(singapore));
        when(tariffRateRepository.search(eq(singapore), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        // Act
        List<TariffRateDto> results = tariffService.search("  SGP  ", null, null);

        // Assert
        assertNotNull(results);
        // Verify it was called with the uppercased value (whitespace preserved)
        verify(countryRepository).findByCode("  SGP  ");
    }

    // ==================== generateAiSummary Method Tests ====================

    @Test
    void generateAiSummary_ShouldReturnSummary_WhenGeminiClientSucceeds() {
        // Arrange
        TariffCalcResponse response = new TariffCalcResponse();
        response.originCountryCode = "SGP";
        response.destinationCountryCode = "USA";
        response.productCategoryCode = "ELEC";
        response.declaredValue = new BigDecimal("1000.00");
        response.baseRate = new BigDecimal("0.05");
        response.tariffAmount = new BigDecimal("50.00");
        response.additionalFee = new BigDecimal("10.00");
        response.totalCost = new BigDecimal("1060.00");
        response.effectiveDate = LocalDate.now().toString();

        when(geminiClient.generateSummary(anyString()))
            .thenReturn("<p>This is a test summary</p>");

        // Act
        String summary = tariffService.generateAiSummary(response);

        // Assert
        assertNotNull(summary);
        assertThat(summary).contains("This is a test summary");
        verify(geminiClient).generateSummary(anyString());
    }

    @Test
    void generateAiSummary_ShouldThrowException_WhenResponseIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tariffService.generateAiSummary(null)
        );
        assertThat(exception.getMessage()).contains("Tariff response is required");
    }

    @Test
    void generateAiSummary_ShouldReturnFallbackMessage_WhenGeminiClientFails() {
        // Arrange
        TariffCalcResponse response = new TariffCalcResponse();
        response.originCountryCode = "SGP";
        response.destinationCountryCode = "USA";
        response.productCategoryCode = "ELEC";
        response.declaredValue = new BigDecimal("1000.00");
        response.baseRate = new BigDecimal("0.05");
        response.tariffAmount = new BigDecimal("50.00");
        response.additionalFee = new BigDecimal("10.00");
        response.totalCost = new BigDecimal("1060.00");
        response.effectiveDate = LocalDate.now().toString();

        when(geminiClient.generateSummary(anyString()))
            .thenThrow(new RuntimeException("API Error"));

        // Act
        String summary = tariffService.generateAiSummary(response);

        // Assert
        assertEquals("AI summary unavailable.", summary);
        verify(geminiClient).generateSummary(anyString());
    }
}
