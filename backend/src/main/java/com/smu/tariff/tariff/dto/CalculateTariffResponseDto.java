package com.smu.tariff.tariff.dto;

import java.math.BigDecimal;

public class CalculateTariffResponseDto {
    private String productCode;
    private String hsCode;
    private boolean weightBased;
    private double weight;
    private BigDecimal calculatedTariff;
    private String currency;

    public CalculateTariffResponseDto() { }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getHsCode() { return hsCode; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }

    public boolean isWeightBased() { return weightBased; }
    public void setWeightBased(boolean weightBased) { this.weightBased = weightBased; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public BigDecimal getCalculatedTariff() { return calculatedTariff; }
    public void setCalculatedTariff(BigDecimal calculatedTariff) { this.calculatedTariff = calculatedTariff; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
