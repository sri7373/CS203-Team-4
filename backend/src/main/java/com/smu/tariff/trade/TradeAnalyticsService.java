package com.smu.tariff.trade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.tariff.TariffRate;
import com.smu.tariff.tariff.TariffRateRepository;
import com.smu.tariff.trade.dto.CountryTradeInsightsDto;
import com.smu.tariff.trade.dto.PartnerMetricDto;
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

        // Generate major trading partners (countries with most tariff relationships)
        dto.majorPartners = generateTradingPartners(importTariffs, exportTariffs);

        // Calculate average tariffs
        dto.averageImportTariff = computeAverageTariff(importTariffs);
        dto.averageExportTariff = computeAverageTariff(exportTariffs);
        
        logger.info("Average import tariff={}", dto.averageImportTariff);
        logger.info("Average export tariff={}", dto.averageExportTariff);
        logger.info("Trade insights generation completed for country={}", normalizedCountryCode);

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
                            .map(TariffRate::getBaseRate)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    
                    if (!baseRates.isEmpty()) {
                        dto.baseRate = baseRates.stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .divide(BigDecimal.valueOf(baseRates.size()), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)); // Convert to percentage
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
                    
                    // Use base rate percentage as the primary sorting value
                    dto.totalValue = dto.baseRate;
                    
                    return dto;
                })
                .sorted((a, b) -> b.totalValue.compareTo(a.totalValue))
                .limit(MAX_ITEMS)
                .collect(Collectors.toList());
    }

    private List<PartnerMetricDto> generateTradingPartners(List<TariffRate> importTariffs, 
                                                          List<TariffRate> exportTariffs) {
        Map<String, PartnerMetricDto> partnerMap = new HashMap<>();
        
        // Count import partners and calculate average base rate percentage
        Map<String, List<TariffRate>> importPartners = importTariffs.stream()
                .collect(Collectors.groupingBy(t -> t.getOrigin().getCode()));
        
        for (Map.Entry<String, List<TariffRate>> entry : importPartners.entrySet()) {
            String code = entry.getKey();
            List<TariffRate> rates = entry.getValue();
            
            PartnerMetricDto dto = new PartnerMetricDto();
            dto.code = code;
            dto.name = rates.get(0).getOrigin().getName();
            
            // Calculate average base rate as percentage - properly handle multiple rates
            BigDecimal sum = rates.stream()
                    .map(TariffRate::getBaseRate)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal avgBaseRate = sum.divide(BigDecimal.valueOf(rates.size()), 6, RoundingMode.HALF_UP);
            
            // Convert to percentage
            BigDecimal avgBaseRatePercent = avgBaseRate.multiply(BigDecimal.valueOf(100));
            
            dto.totalValue = avgBaseRatePercent;
            dto.rateCount = rates.size(); // Track how many rates went into this calculation
            partnerMap.put(code, dto);
        }

        // Add export partners and their average tariff rates
        Map<String, List<TariffRate>> exportPartners = exportTariffs.stream()
                .collect(Collectors.groupingBy(t -> t.getDestination().getCode()));
        
        for (Map.Entry<String, List<TariffRate>> entry : exportPartners.entrySet()) {
            String code = entry.getKey();
            List<TariffRate> rates = entry.getValue();
            
            // Calculate average base rate as percentage - properly handle multiple rates
            BigDecimal sum = rates.stream()
                    .map(TariffRate::getBaseRate)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal avgBaseRate = sum.divide(BigDecimal.valueOf(rates.size()), 6, RoundingMode.HALF_UP);
            
            BigDecimal avgBaseRatePercent = avgBaseRate.multiply(BigDecimal.valueOf(100));
            
            PartnerMetricDto existing = partnerMap.get(code);
            if (existing != null) {
                // Properly combine import and export averages
                // Take weighted average based on the number of rates
                BigDecimal combinedSum = existing.totalValue.multiply(BigDecimal.valueOf(existing.rateCount))
                        .add(avgBaseRatePercent.multiply(BigDecimal.valueOf(rates.size())));
                int totalRates = existing.rateCount + rates.size();
                existing.totalValue = combinedSum.divide(BigDecimal.valueOf(totalRates), 4, RoundingMode.HALF_UP);
                existing.rateCount = totalRates;
            } else {
                PartnerMetricDto dto = new PartnerMetricDto();
                dto.code = code;
                dto.name = rates.get(0).getDestination().getName();
                dto.totalValue = avgBaseRatePercent;
                dto.rateCount = rates.size();
                partnerMap.put(code, dto);
            }
        }

        return partnerMap.values().stream()
                .sorted((a, b) -> b.totalValue.compareTo(a.totalValue))
                .limit(MAX_ITEMS)
                .collect(Collectors.toList());
    }
}
