package com.smu.tariff.product;

import java.math.BigDecimal;

public class ImportDutyDto {
    private String hscode;
    private String originCountry;
    private String destCountry;
    private BigDecimal baseRate;
    private BigDecimal additionalFee;
    private BigDecimal ftaRate;
    private BigDecimal totalDuty;

    public ImportDutyDto() { }

    public String getHscode() { return hscode; }
    public void setHscode(String hscode) { this.hscode = hscode; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getDestCountry() { return destCountry; }
    public void setDestCountry(String destCountry) { this.destCountry = destCountry; }

    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }

    public BigDecimal getAdditionalFee() { return additionalFee; }
    public void setAdditionalFee(BigDecimal additionalFee) { this.additionalFee = additionalFee; }

    public BigDecimal getFtaRate() { return ftaRate; }
    public void setFtaRate(BigDecimal ftaRate) { this.ftaRate = ftaRate; }

    public BigDecimal getTotalDuty() { return totalDuty; }
    public void setTotalDuty(BigDecimal totalDuty) { this.totalDuty = totalDuty; }
}
