package com.smu.tariff.trade;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.TariffRateRepository;
import com.smu.tariff.trade.dto.CountryTradeInsightsDto;
import com.smu.tariff.exception.InvalidTariffRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradeAnalyticsServiceUnitTest {

    private CountryRepository countryRepository;
    private TariffRateRepository tariffRateRepository;
    private TradeAnalyticsService service;

    @BeforeEach
    void setUp() {
        countryRepository = mock(CountryRepository.class);
        tariffRateRepository = mock(TariffRateRepository.class);
        service = new TradeAnalyticsService(countryRepository, tariffRateRepository);
    }

    @Test
    void getCountryInsights_shouldThrow_whenCountryCodeBlank() {
        assertThrows(InvalidTariffRequestException.class, () -> service.getCountryInsights("  "));
    }

    @Test
    void getCountryInsights_happyPath_returnsDto() {
        Country country = new Country("SGP", "Singapore");
        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(country));

        ProductCategory cat = new ProductCategory();
        cat.setCode("ELEC");
        cat.setName("Electronics");

        TariffRate rate = new TariffRate();
        rate.setOrigin(new Country("CHN", "China"));
        rate.setDestination(country);
        rate.setProductCategory(cat);
        rate.setBaseRate(BigDecimal.valueOf(0.05));
        rate.setAdditionalFee(BigDecimal.valueOf(1.0));
        rate.setEffectiveFrom(LocalDate.now());

        when(tariffRateRepository.search(null, country, null)).thenReturn(List.of(rate));
        when(tariffRateRepository.search(country, null, null)).thenReturn(List.of(rate));

        CountryTradeInsightsDto dto = service.getCountryInsights("sgp");

        assertNotNull(dto);
        assertEquals("SGP", dto.countryCode);
        assertEquals("Singapore", dto.countryName);
        assertNotNull(dto.topImports);
        assertFalse(dto.topImports.isEmpty());
        assertNotNull(dto.averageImportTariff);
        assertTrue(dto.averageImportTariff.compareTo(BigDecimal.ZERO) > 0);
    }
}
