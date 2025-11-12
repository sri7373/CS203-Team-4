package com.smu.tariff.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.TariffRateRepository;
import com.smu.tariff.trade.dto.CountryTradeInsightsDto;

@ExtendWith(MockitoExtension.class)
class TradeAnalyticsServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private TariffRateRepository tariffRateRepository;

    @InjectMocks
    private TradeAnalyticsService service;

    private Country singapore;
    private Country unitedStates;
    private Country china;
    private ProductCategory electronics;
    private ProductCategory apparel;

    @BeforeEach
    void setUp() {
        singapore = new Country("SGP", "Singapore");
        unitedStates = new Country("USA", "United States");
        china = new Country("CHN", "China");
        electronics = new ProductCategory("ELEC", "Electronics");
        apparel = new ProductCategory("APP", "Apparel");
    }

    @Test
    void getCountryInsightsAggregatesRatesAndPartners() {
        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(singapore));

        var importRates = List.of(
                rate(unitedStates, singapore, electronics, "5.0000", "10.00"),
                rate(unitedStates, singapore, apparel, "7.0000", "6.00"),
                rate(china, singapore, electronics, "3.0000", "4.00")
        );
        var exportRates = List.of(
                rate(singapore, unitedStates, electronics, "2.0000", "2.50"),
                rate(singapore, china, apparel, "4.0000", "3.50")
        );

        when(tariffRateRepository.search(null, singapore, null)).thenReturn(importRates);
        when(tariffRateRepository.search(singapore, null, null)).thenReturn(exportRates);

        CountryTradeInsightsDto dto = service.getCountryInsights("sgp");

        assertThat(dto.countryCode).isEqualTo("SGP");
        assertThat(dto.averageImportTariff).isEqualByComparingTo("5.0000"); // average of 5,7,3
        assertThat(dto.averageExportTariff).isEqualByComparingTo("3.0000");

        assertThat(dto.topImports).hasSize(2);
        assertThat(dto.topImports.get(0).code).isEqualTo("APP");
        assertThat(dto.topImports.get(0).baseRate).isEqualByComparingTo("7.0000");
        assertThat(dto.topImports.get(0).additionalFee).isEqualByComparingTo("6.00");

        assertThat(dto.majorImportPartners).hasSize(2);
        assertThat(dto.majorImportPartners.get(0).code).isEqualTo("USA");
        assertThat(dto.majorImportPartners.get(0).itemCount).isEqualTo(2);
        assertThat(dto.majorImportPartners.get(0).items.get(0).baseRate).isEqualByComparingTo("7.0000");

        assertThat(dto.majorExportPartners).extracting(p -> p.code)
                .containsExactlyInAnyOrder("USA", "CHN");
    }

    @Test
    void getCountryInsightsRejectsBlankCountryCode() {
        assertThatThrownBy(() -> service.getCountryInsights("   "))
                .isInstanceOf(InvalidTariffRequestException.class)
                .hasMessageContaining("Country code is required");
    }

    @Test
    void getCountryInsightsRejectsUnknownCountryCode() {
        when(countryRepository.findByCode("SGP")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCountryInsights("SGP"))
                .isInstanceOf(InvalidTariffRequestException.class)
                .hasMessageContaining("Unknown country code");
    }

    private TariffRate rate(Country origin,
                            Country destination,
                            ProductCategory category,
                            String baseRate,
                            String fee) {
        TariffRate rate = new TariffRate();
        rate.setOrigin(origin);
        rate.setDestination(destination);
        rate.setProductCategory(category);
        rate.setBaseRate(new BigDecimal(baseRate));
        rate.setAdditionalFee(new BigDecimal(fee));
        rate.setEffectiveFrom(LocalDate.now());
        return rate;
    }
}
