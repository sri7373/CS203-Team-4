package com.smu.tariff.tariff.dto;

import java.math.BigDecimal;

public class TariffCalcResponse {
    public String originCountryCode;
    public String destinationCountryCode;
    public String productCategoryCode;
    public String effectiveDate;
    public BigDecimal declaredValue;
    public BigDecimal baseRate; // e.g., 0.0500 for 5%
    public BigDecimal tariffAmount;
    public BigDecimal additionalFee;
    public BigDecimal totalCost;
    public String notes;
    public String aiSummary; //strings together the related data
    public List<String> relatedNews;// headlines/links

    public TariffCalcResponse() { }
}
