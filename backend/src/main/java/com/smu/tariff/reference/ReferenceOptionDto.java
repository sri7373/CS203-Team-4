package com.smu.tariff.reference;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferenceOptionDto {
    public final String code;
    public final String label;

    public ReferenceOptionDto(String code, String label) {
        this.code = code;
        this.label = label;
    }

    // Backwards-compatible JSON property name for existing clients/tests that
    // expect a `name` field. It mirrors `label`.
    @JsonProperty("name")
    public String getName() {
        return label;
    }
}
