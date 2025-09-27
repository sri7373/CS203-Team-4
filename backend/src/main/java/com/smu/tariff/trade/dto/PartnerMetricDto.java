package com.smu.tariff.trade.dto;

import java.math.BigDecimal;

public class PartnerMetricDto {
    public String code;
    public String name;
    public BigDecimal totalValue;
    public int rateCount; // Track number of tariff rates used in calculation
}
