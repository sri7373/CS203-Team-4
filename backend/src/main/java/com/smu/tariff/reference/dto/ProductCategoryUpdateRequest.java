package com.smu.tariff.reference.dto;

import jakarta.validation.constraints.NotBlank;

public class ProductCategoryUpdateRequest {

    @NotBlank
    public String name;

    @NotBlank
    public String hsCode;

    public Boolean weightBased;

    /**
     * Optional new category code. If omitted, the original code is preserved.
     */
    public String code;
}
