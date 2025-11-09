package com.smu.tariff.tariff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CalculateTariffRequestDto {
    @NotBlank
    private String productCode;

    @NotBlank
    private String originCountry;

    @NotBlank
    private String destCountry;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private Double declaredValue;

    private Double quantity;
    private Double weight;

    public CalculateTariffRequestDto() { }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getDestCountry() { return destCountry; }
    public void setDestCountry(String destCountry) { this.destCountry = destCountry; }

    public Double getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(Double declaredValue) { this.declaredValue = declaredValue; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}
