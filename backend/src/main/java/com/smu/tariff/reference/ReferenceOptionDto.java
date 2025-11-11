package com.smu.tariff.reference;

public class ReferenceOptionDto {
    public final String code;
    public final String name;
    public final String hsCode;
    public final Boolean weightBased;

    public ReferenceOptionDto(String code, String name) {
        this(code, name, null, null);
    }

    public ReferenceOptionDto(String code, String name, String hsCode, Boolean weightBased) {
        this.code = code;
        this.name = name;
        this.hsCode = hsCode;
        this.weightBased = weightBased;
    }
}
