package com.smu.tariff.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.TariffRateRepository;
import com.smu.tariff.tariff.BaseRateUtils;
import com.smu.tariff.trade.dto.CountryTradeInsightsDto;
import com.smu.tariff.trade.dto.PartnerTradeDetailsDto;
import com.smu.tariff.trade.dto.PartnerTradeItemDto;
import com.smu.tariff.trade.dto.ProductMetricDto;

@Service
@Transactional(readOnly = true)
public class TradeAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(TradeAnalyticsService.class);
    private static final int MAX_ITEMS = 5;

    private final CountryRepository countryRepository;
    private final TariffRateRepository tariffRateRepository;

    public TradeAnalyticsService(CountryRepository countryRepository,
                                 TariffRateRepository tariffRateRepository) {
        this.countryRepository = countryRepository;
        this.tariffRateRepository = tariffRateRepository;
    }

    public CountryTradeInsightsDto getCountryInsights(String countryCode) {
        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new InvalidTariffRequestException("Country code is required");
        }

        String normalizedCountryCode = countryCode.trim().toUpperCase();
        Country country = countryRepository.findByCode(normalizedCountryCode)
                .orElseThrow(() -> new InvalidTariffRequestException("Unknown country code: " + countryCode));

        logger.info("Generating trade insights for country={}", normalizedCountryCode);

        CountryTradeInsightsDto dto = new CountryTradeInsightsDto();
        dto.countryCode = country.getCode();
        dto.countryName = country.getName();

        // Generate insights from existing tariff rate data
        List<TariffRate> importTariffs = tariffRateRepository.search(null, country, null);  // Tariffs TO this country
        List<TariffRate> exportTariffs = tariffRateRepository.search(country, null, null);  // Tariffs FROM this country

        logger.debug("Found {} import tariff rates for {}", importTariffs.size(), normalizedCountryCode);
        logger.debug("Found {} export tariff rates for {}", exportTariffs.size(), normalizedCountryCode);
        
        // Log sample data to debug null values
        if (!importTariffs.isEmpty()) {
            TariffRate sample = importTariffs.get(0);
            logger.debug("Sample import tariff: id={}, origin={}, destination={}, baseRate={}, additionalFee={}, product={} ({})",
                    sample.getId(),
                    sample.getOrigin().getCode(),
                    sample.getDestination().getCode(),
                    sample.getBaseRate(),
                    sample.getAdditionalFee(),
                    sample.getProductCategory().getName(),
                    sample.getProductCategory().getCode());
        }

        // Generate top import categories (highest tariff rates TO this country)
        dto.topImports = generateTopProducts(importTariffs);
        
        // Generate top export categories (tariff rates FROM this country)  
        dto.topExports = generateTopProducts(exportTariffs);

        // Generate major trading partners for imports and exports
        dto.majorImportPartners = generatePartnerDetails(importTariffs, true);
        dto.majorExportPartners = generatePartnerDetails(exportTariffs, false);

        // Calculate average tariffs
        dto.averageImportTariff = computeAverageTariff(importTariffs);
        dto.averageExportTariff = computeAverageTariff(exportTariffs);
        
        logger.info("Average import tariff={}", dto.averageImportTariff);
        logger.info("Average export tariff={}", dto.averageExportTariff);
        logger.info("Trade insights generation completed for country={}", normalizedCountryCode);

        return dto;
    }

    private BigDecimal computeAverageTariff(List<TariffRate> rates) {
        BigDecimal zero = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        if (rates == null || rates.isEmpty()) {
            return zero;
        }
        List<BigDecimal> baseRates = rates.stream()
                .map(this::normalizedBaseRate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (baseRates.isEmpty()) {
            return zero;
        }
        BigDecimal sum = baseRates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageDecimal = sum.divide(BigDecimal.valueOf(baseRates.size()), 6, RoundingMode.HALF_UP);
        return BaseRateUtils.toStoredPercentage(averageDecimal);
    }

    private List<ProductMetricDto> generateTopProducts(List<TariffRate> tariffRates) {
        logger.debug("generateTopProducts called with {} tariff rates", tariffRates.size());
        
        // Group by product category and calculate average tariff metrics
        Map<String, List<TariffRate>> productGroups = tariffRates.stream()
                .collect(Collectors.groupingBy(t -> t.getProductCategory().getCode()));

        logger.debug("Found {} product categories", productGroups.size());

        return productGroups.entrySet().stream()
                .map(entry -> {
                    String productCode = entry.getKey();
                    List<TariffRate> rates = entry.getValue();
                    
                    logger.debug("Processing product {} with {} rates", productCode, rates.size());
                    
                    ProductMetricDto dto = new ProductMetricDto();
                    dto.code = productCode;
                    dto.name = rates.get(0).getProductCategory().getName();
                    
                    // Calculate average base rate (as percentage) - handle nulls
                    List<BigDecimal> baseRates = rates.stream()
                            .map(this::normalizedBaseRate)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    
                    if (!baseRates.isEmpty()) {
                        BigDecimal avgDecimal = baseRates.stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(baseRates.size()), 6, RoundingMode.HALF_UP);
                        dto.baseRate = BaseRateUtils.toStoredPercentage(avgDecimal);
                    } else {
                        dto.baseRate = null;
                        logger.warn("No valid base rates found for product {}", productCode);
                    }
                    
                    // Calculate average additional fee - handle nulls
                    List<BigDecimal> additionalFees = rates.stream()
                            .map(TariffRate::getAdditionalFee)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    
                    if (!additionalFees.isEmpty()) {
                        dto.additionalFee = additionalFees.stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(additionalFees.size()), 2, RoundingMode.HALF_UP);
                    } else {
                        dto.additionalFee = null;
                        logger.warn("No valid additional fees found for product {}", productCode);
                    }
                    
                    logger.debug("Product {} summary: baseRate={}, additionalFee={}", productCode, dto.baseRate, dto.additionalFee);
                    
                    dto.totalValue = dto.baseRate != null ? dto.baseRate : BigDecimal.ZERO;
                    
                    return dto;
                })
                .filter(dto -> dto.totalValue != null)
                .sorted((a, b) -> b.totalValue.compareTo(a.totalValue))
                .limit(MAX_ITEMS)
                .collect(Collectors.toList());
    }

    private List<PartnerTradeDetailsDto> generatePartnerDetails(List<TariffRate> tariffs, boolean isImport) {
        Map<String, List<TariffRate>> grouped = tariffs.stream()
                .collect(Collectors.groupingBy(rate -> isImport ? rate.getOrigin().getCode() : rate.getDestination().getCode()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<TariffRate> partnerRates = entry.getValue();
                    PartnerTradeDetailsDto dto = new PartnerTradeDetailsDto();
                    dto.code = entry.getKey();
                    dto.name = isImport
                            ? partnerRates.get(0).getOrigin().getName()
                            : partnerRates.get(0).getDestination().getName();
                    dto.itemCount = partnerRates.size();
                    dto.items = partnerRates.stream()
                            .sorted((a, b) -> b.getBaseRate().compareTo(a.getBaseRate()))
                            .map(rate -> {
                                PartnerTradeItemDto item = new PartnerTradeItemDto();
                                item.categoryCode = rate.getProductCategory().getCode();
                                item.categoryName = rate.getProductCategory().getName();
                                item.baseRate = rate.getBaseRate();
                                item.additionalFee = rate.getAdditionalFee();
                                return item;
                            })
                            .collect(Collectors.toList());
                    return dto;
                })
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.itemCount, a.itemCount);
                    if (cmp != 0) {
                        return cmp;
                    }
                    return a.code.compareTo(b.code);
                })
                .limit(MAX_ITEMS)
                .collect(Collectors.toList());
    }

    private BigDecimal normalizedBaseRate(TariffRate rate) {
        return BaseRateUtils.fromStoredPercentage(rate.getBaseRate());
    }
}

