package com.smu.tariff.reference.dto;

import jakarta.validation.constraints.NotBlank;

public class CountryUpdateRequest {

    @NotBlank
    public String name;

    /**
     * Optional new country code. If null or blank, the existing code is retained.
     */
    public String code;
}
