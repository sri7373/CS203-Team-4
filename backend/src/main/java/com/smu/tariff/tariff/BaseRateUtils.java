package com.smu.tariff.tariff;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BaseRateUtils {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int NORMALIZED_SCALE = 8;
    private static final int STORED_SCALE = 4;

    private BaseRateUtils() {}

    /**
     * Converts a stored percentage value (0-100) into the decimal representation (e.g. 3.4125 -> 0.034125).
     */
    public static BigDecimal fromStoredPercentage(BigDecimal storedValue) {
        if (storedValue == null) {
            return null;
        }
        return storedValue.divide(ONE_HUNDRED, NORMALIZED_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Converts a decimal representation (e.g. 0.034125) into the stored percentage (3.4125).
     */
    public static BigDecimal toStoredPercentage(BigDecimal decimalValue) {
        if (decimalValue == null) {
            return null;
        }
        return decimalValue.multiply(ONE_HUNDRED).setScale(STORED_SCALE, RoundingMode.HALF_UP);
    }
}
