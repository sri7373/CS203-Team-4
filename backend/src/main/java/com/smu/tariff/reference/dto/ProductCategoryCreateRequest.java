package com.smu.tariff.reference.dto;

import jakarta.validation.constraints.NotBlank;

public class ProductCategoryCreateRequest {
    @NotBlank
    public String code;

    @NotBlank
    public String name;

    @NotBlank
    public String hsCode;

    public boolean weightBased;
}
