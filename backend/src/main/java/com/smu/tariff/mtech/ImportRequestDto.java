package com.smu.tariff.mtech;

public class ImportRequestDto {
    private String url;

    public ImportRequestDto() { }

    public ImportRequestDto(String url) { this.url = url; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
