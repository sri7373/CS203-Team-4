package com.smu.tariff.trade;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.tariff.TariffRate;
import com.smu.tariff.tariff.TariffRateRepository;
import com.smu.tariff.trade.dto.CountryTradeInsightsDto;
import com.smu.tariff.trade.dto.PartnerMetricDto;
import com.smu.tariff.trade.dto.ProductMetricDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TradeAnalyticsService {

    private static final int MAX_ITEMS = 5;

    private final CountryRepository countryRepository;
    private final TradeFlowRepository tradeFlowRepository;
    private final TariffRateRepository tariffRateRepository;

    public TradeAnalyticsService(CountryRepository countryRepository,
                                 TradeFlowRepository tradeFlowRepository,
                                 TariffRateRepository tariffRateRepository) {
        this.countryRepository = countryRepository;
        this.tradeFlowRepository = tradeFlowRepository;
        this.tariffRateRepository = tariffRateRepository;
    }

    public CountryTradeInsightsDto getCountryInsights(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Country code is required");
        }

        Country country = countryRepository.findByCode(countryCode.trim().toUpperCase())
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown country code: " + countryCode));

        CountryTradeInsightsDto dto = new CountryTradeInsightsDto();
        dto.countryCode = country.getCode();
        dto.countryName = country.getName();

        dto.topImports = tradeFlowRepository
                .findTopProductsByCountryAndDirection(country, TradeDirection.IMPORT)
                .stream()
                .limit(MAX_ITEMS)
                .map(this::toProductMetric)
                .collect(Collectors.toList());

        dto.topExports = tradeFlowRepository
                .findTopProductsByCountryAndDirection(country, TradeDirection.EXPORT)
                .stream()
                .limit(MAX_ITEMS)
                .map(this::toProductMetric)
                .collect(Collectors.toList());

        dto.majorPartners = tradeFlowRepository
                .findPartnersByCountry(country)
                .stream()
                .limit(MAX_ITEMS)
                .map(this::toPartnerMetric)
                .collect(Collectors.toList());

        dto.averageImportTariff = computeAverageTariff(tariffRateRepository.search(null, country, null));
        dto.averageExportTariff = computeAverageTariff(tariffRateRepository.search(country, null, null));

        return dto;
    }

    private BigDecimal computeAverageTariff(List<TariffRate> rates) {
        if (rates == null || rates.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal sum = rates.stream()
                .map(TariffRate::getBaseRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(rates.size()), 4, RoundingMode.HALF_UP);
    }

    private ProductMetricDto toProductMetric(TradeFlowRepository.ProductSummary summary) {
        ProductMetricDto dto = new ProductMetricDto();
        dto.code = summary.getCode();
        dto.name = summary.getName();
        dto.totalValue = summary.getTotalValue();
        return dto;
    }

    private PartnerMetricDto toPartnerMetric(TradeFlowRepository.PartnerSummary summary) {
        PartnerMetricDto dto = new PartnerMetricDto();
        dto.code = summary.getCode();
        dto.name = summary.getName();
        dto.totalValue = summary.getTotalValue();
        return dto;
    }
}
