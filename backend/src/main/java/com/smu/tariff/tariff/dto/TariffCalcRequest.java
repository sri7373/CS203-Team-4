package com.smu.tariff.tariff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TariffCalcRequest {
    @NotBlank public String originCountryCode;
    @NotBlank public String destinationCountryCode;
    @NotBlank public String hsCode;
    public String productCategoryCode;

    @NotNull @DecimalMin(value = "0.0", inclusive = false)
    public Double declaredValue; // base product value BEFORE weight adjustments

    @DecimalMin(value = "0.0", inclusive = false)
    public Double weight; // optional; required if the product category is weight based

    public String effectiveFrom; // ISO yyyy-MM-dd optional filter
    public String effectiveTo;   // ISO yyyy-MM-dd optional filter
}
