package com.smu.tariff.product;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MtechHsCodeEntry {
    @JsonProperty("searchTerm")
    private String searchTerm;

    @JsonProperty("category")
    private String category;

    @JsonProperty("6DigitCode")
    private String sixDigitCode;

    @JsonProperty("desc")
    private String desc;

    @JsonProperty("HSCodeAccuracy")
    private Integer hsCodeAccuracy;

    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSixDigitCode() { return sixDigitCode; }
    public void setSixDigitCode(String sixDigitCode) { this.sixDigitCode = sixDigitCode; }

    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }

    public Integer getHsCodeAccuracy() { return hsCodeAccuracy; }
    public void setHsCodeAccuracy(Integer hsCodeAccuracy) { this.hsCodeAccuracy = hsCodeAccuracy; }
}
