package com.smu.tariff.tariff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TariffCalcRequest {
    @NotBlank public String originCountryCode;
    @NotBlank public String destinationCountryCode;
    @NotBlank public String productCategoryCode;

    @NotNull @DecimalMin(value = "0.0", inclusive = false)
    public Double declaredValue; // base product value in destination currency

    public String date; // ISO date yyyy-MM-dd; optional -> today
}
