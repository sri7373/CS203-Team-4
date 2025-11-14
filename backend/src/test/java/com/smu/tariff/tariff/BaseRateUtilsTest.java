package com.smu.tariff.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BaseRateUtilsTest {

    @Test
    void fromStoredPercentageHandlesNegativeAndZero() {
        assertThat(BaseRateUtils.fromStoredPercentage(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO.setScale(8));
        assertThat(BaseRateUtils.fromStoredPercentage(new BigDecimal("-5.0000"))).isEqualByComparingTo(new BigDecimal("-0.05000000"));
    }

    @Test
    void fromStoredPercentageHandlesLargeValue() {
        assertThat(BaseRateUtils.fromStoredPercentage(new BigDecimal("123456.7890"))).isEqualByComparingTo(new BigDecimal("1234.56789000"));
    }

    @Test
    void toStoredPercentageHandlesNegativeAndZero() {
        assertThat(BaseRateUtils.toStoredPercentage(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO.setScale(4));
        assertThat(BaseRateUtils.toStoredPercentage(new BigDecimal("-0.05"))).isEqualByComparingTo(new BigDecimal("-5.0000"));
    }

    @Test
    void toStoredPercentageHandlesLargeValue() {
        assertThat(BaseRateUtils.toStoredPercentage(new BigDecimal("1234.56789"))).isEqualByComparingTo(new BigDecimal("123456.7890"));
    }

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
