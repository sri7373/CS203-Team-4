package com.smu.tariff.trade.dto;

import java.math.BigDecimal;
import java.util.List;

public class CountryTradeInsightsDto {
    public String countryCode;
    public String countryName;
    public BigDecimal averageImportTariff;
    public BigDecimal averageExportTariff;
    public List<ProductMetricDto> topImports;
    public List<ProductMetricDto> topExports;
    public List<PartnerMetricDto> majorPartners;
}
