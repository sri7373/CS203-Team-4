package com.smu.tariff.product;

import java.math.BigDecimal;

public class MtechImportDutyResponse {
    private Data data;

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        private BigDecimal baseRate;
        private BigDecimal additionalFee;
        private BigDecimal ftaRate;
        private BigDecimal totalDuty;

        public BigDecimal getBaseRate() { return baseRate; }
        public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }

        public BigDecimal getAdditionalFee() { return additionalFee; }
        public void setAdditionalFee(BigDecimal additionalFee) { this.additionalFee = additionalFee; }

        public BigDecimal getFtaRate() { return ftaRate; }
        public void setFtaRate(BigDecimal ftaRate) { this.ftaRate = ftaRate; }

        public BigDecimal getTotalDuty() { return totalDuty; }
        public void setTotalDuty(BigDecimal totalDuty) { this.totalDuty = totalDuty; }
    }
}
