package com.smu.tariff.tariff.dto;

import java.math.BigDecimal;

public class TariffCalcResponse {
    public String originCountryCode;
    public String destinationCountryCode;
    public String hsCode;
    public String productCategoryCode;
    public String productCategoryName;
    public Boolean weightBased;
    public Double weight;
    public String rateEffectiveFrom;
    public String rateEffectiveTo;
    public String requestedEffectiveFrom;
    public String requestedEffectiveTo;
    public String effectiveDate; // legacy field for backwards compatibility
    public BigDecimal declaredValue;
    public BigDecimal declaredValuePerUnit;
    public BigDecimal baseRate; // percentage value as stored (e.g., 5.0000 for 5%)
    public BigDecimal tariffAmount;
    public BigDecimal additionalFee;
    public BigDecimal totalCost;
    public String notes;
    public String aiSummary; // Contains an AI-generated HTML summary of the tariff calculation


    public TariffCalcResponse() { }
}
