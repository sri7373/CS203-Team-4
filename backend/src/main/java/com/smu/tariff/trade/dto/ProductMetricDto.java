package com.smu.tariff.trade.dto;

import java.math.BigDecimal;

public class ProductMetricDto {
    public String code;
    public String name;
    public BigDecimal totalValue; // Keep for sorting purposes
    public BigDecimal baseRate; // Average base rate as percentage
    public BigDecimal additionalFee; // Average additional fee in dollars
}
