package com.smu.tariff.tariff.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TariffRateDtoPost {
    public String originCountryCode;
    public String destinationCountryCode;
    public String productCategoryCode;
    public BigDecimal baseRate;
    public BigDecimal additionalFee;
    public LocalDate effectiveFrom;
    public LocalDate effectiveTo;
}