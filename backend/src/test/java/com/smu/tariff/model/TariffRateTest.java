package com.smu.tariff.model;
import com.smu.tariff.country.Country;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class TariffRateTest {
    @Test
    void testGettersAndSetters() {
        TariffRate rate = new TariffRate();
        Country origin = new Country("SGP", "Singapore");
        Country dest = new Country("USA", "United States");
        ProductCategory cat = new ProductCategory("ELEC", "Electronics");
        BigDecimal base = new BigDecimal("5.0000");
        BigDecimal fee = new BigDecimal("10.00");
        LocalDate from = LocalDate.of(2023, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 1);
        rate.setOrigin(origin);
        rate.setDestination(dest);
        rate.setProductCategory(cat);
        rate.setBaseRate(base);
        rate.setAdditionalFee(fee);
        rate.setWeightValue(2.5f);
        rate.setEffectiveFrom(from);
        rate.setEffectiveTo(to);
        assertThat(rate.getOrigin()).isEqualTo(origin);
        assertThat(rate.getDestination()).isEqualTo(dest);
        assertThat(rate.getProductCategory()).isEqualTo(cat);
        assertThat(rate.getBaseRate()).isEqualTo(base);
        assertThat(rate.getAdditionalFee()).isEqualTo(fee);
        assertThat(rate.getWeightValue()).isEqualTo(2.5f);
        assertThat(rate.getEffectiveFrom()).isEqualTo(from);
        assertThat(rate.getEffectiveTo()).isEqualTo(to);
    }

    @Test
    void testAllArgsConstructor() {
        Country origin = new Country("SGP", "Singapore");
        Country dest = new Country("USA", "United States");
        ProductCategory cat = new ProductCategory("ELEC", "Electronics");
        BigDecimal base = new BigDecimal("5.0000");
        BigDecimal fee = new BigDecimal("10.00");
        LocalDate from = LocalDate.of(2023, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 1);
        TariffRate rate = new TariffRate(origin, dest, cat, base, fee, from, to);
        assertThat(rate.getOrigin()).isEqualTo(origin);
        assertThat(rate.getDestination()).isEqualTo(dest);
        assertThat(rate.getProductCategory()).isEqualTo(cat);
        assertThat(rate.getBaseRate()).isEqualTo(base);
        assertThat(rate.getAdditionalFee()).isEqualTo(fee);
        assertThat(rate.getEffectiveFrom()).isEqualTo(from);
        assertThat(rate.getEffectiveTo()).isEqualTo(to);
    }
}
