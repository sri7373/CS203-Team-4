package com.smu.tariff.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BaseRateUtilsTest {

    @Test
    void fromStoredPercentageConvertsToDecimal() {
        BigDecimal stored = new BigDecimal("5.1234");

        BigDecimal decimal = BaseRateUtils.fromStoredPercentage(stored);

        assertThat(decimal).isEqualByComparingTo(new BigDecimal("0.05123400"));
    }

    @Test
    void fromStoredPercentageReturnsNullWhenInputNull() {
        assertThat(BaseRateUtils.fromStoredPercentage(null)).isNull();
    }

    @Test
    void toStoredPercentageConvertsFromDecimal() {
        BigDecimal decimal = new BigDecimal("0.034125");

        BigDecimal stored = BaseRateUtils.toStoredPercentage(decimal);

        assertThat(stored).isEqualByComparingTo(new BigDecimal("3.4125"));
    }

    @Test
    void toStoredPercentageReturnsNullWhenInputNull() {
        assertThat(BaseRateUtils.toStoredPercentage(null)).isNull();
    }
}
