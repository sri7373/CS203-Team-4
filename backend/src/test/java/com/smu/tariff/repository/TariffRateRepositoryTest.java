package com.smu.tariff.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class TariffRateRepositoryTest {

    @Autowired
    TariffRateRepository tariffRateRepository;

    @Autowired
    CountryRepository countryRepository;

    @Autowired
    ProductCategoryRepository productCategoryRepository;

    private Country singapore;
    private Country unitedStates;
    private Country china;
    private ProductCategory electronics;
    private ProductCategory textiles;

    @BeforeEach
    void setUp() {
        tariffRateRepository.deleteAll();
        productCategoryRepository.deleteAll();
        countryRepository.deleteAll();

        singapore = countryRepository.save(new Country("SGP", "Singapore"));
        unitedStates = countryRepository.save(new Country("USA", "United States"));
        china = countryRepository.save(new Country("CHN", "China"));

        electronics = productCategoryRepository.save(new ProductCategory("ELEC", "Electronics"));
        textiles = productCategoryRepository.save(new ProductCategory("TEXT", "Textiles"));
    }

    @Test
    void searchFiltersByParameters() {
        TariffRate matching = saveRate(singapore, unitedStates, electronics, "5.0000", "12.00", LocalDate.now(), null);
        saveRate(china, unitedStates, electronics, "7.0000", "15.00", LocalDate.now(), null);
        saveRate(singapore, unitedStates, textiles, "4.0000", "5.00", LocalDate.now(), null);

        List<TariffRate> result = tariffRateRepository.search(singapore, unitedStates, electronics);

        assertThat(result).containsExactly(matching);
    }

    @Test
    void findApplicableRatesHonorsEffectiveWindow() {
        LocalDate jan1 = LocalDate.of(2024, 1, 1);
        LocalDate feb1 = LocalDate.of(2024, 2, 1);
        saveRate(singapore, unitedStates, electronics, "5.0000", "12.00", jan1, LocalDate.of(2024, 1, 31));
        TariffRate active = saveRate(singapore, unitedStates, electronics, "6.0000", "14.00", feb1, null);

        List<TariffRate> result = tariffRateRepository.findApplicableRates(
                singapore, unitedStates, electronics, LocalDate.of(2024, 3, 15));

        assertThat(result).containsExactly(active);
    }

    @Test
    void existsByOriginDestinationCategoryAndDate() {
        LocalDate effectiveFrom = LocalDate.of(2024, 6, 1);
        saveRate(singapore, china, textiles, "8.0000", "20.00", effectiveFrom, null);

        boolean exists = tariffRateRepository.existsByOriginAndDestinationAndProductCategoryAndEffectiveFrom(
                singapore, china, textiles, effectiveFrom);

        assertThat(exists).isTrue();
    }

    private TariffRate saveRate(Country origin,
                                Country destination,
                                ProductCategory category,
                                String baseRate,
                                String fee,
                                LocalDate effectiveFrom,
                                LocalDate effectiveTo) {
        TariffRate rate = new TariffRate();
        rate.setOrigin(origin);
        rate.setDestination(destination);
        rate.setProductCategory(category);
        rate.setBaseRate(new BigDecimal(baseRate));
        rate.setAdditionalFee(new BigDecimal(fee));
        rate.setEffectiveFrom(effectiveFrom);
        rate.setEffectiveTo(effectiveTo);
        return tariffRateRepository.save(rate);
    }
}
