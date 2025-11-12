package com.smu.tariff.reference.dto;

import jakarta.validation.constraints.NotBlank;

public class CountryCreateRequest {
    @NotBlank
    public String code;

    @NotBlank
    public String name;
}
