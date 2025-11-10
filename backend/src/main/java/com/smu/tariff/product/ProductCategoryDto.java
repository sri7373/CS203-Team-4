package com.smu.tariff.product;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductCategoryDto {
    private String code;
    private String name;

    @JsonProperty("hs_code")
    private String hsCode;

    @JsonProperty("weight_based")
    private boolean weightBased;

    public ProductCategoryDto() { }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHsCode() { return hsCode; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }

    public boolean isWeightBased() { return weightBased; }
    public void setWeightBased(boolean weightBased) { this.weightBased = weightBased; }
}
