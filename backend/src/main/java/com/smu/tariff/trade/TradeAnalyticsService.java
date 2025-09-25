package com.smu.tariff.trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        System.out.println("Generating trade insights from existing tariff data for country: " + normalizedCountryCode);

        CountryTradeInsightsDto dto = new CountryTradeInsightsDto();
        dto.countryCode = country.getCode();
        dto.countryName = country.getName();

        // Generate insights from existing tariff rate data
        List<TariffRate> importTariffs = tariffRateRepository.search(null, country, null);  // Tariffs TO this country
        List<TariffRate> exportTariffs = tariffRateRepository.search(country, null, null);  // Tariffs FROM this country

        System.out.println("Found " + importTariffs.size() + " import tariff rates for " + normalizedCountryCode);
        System.out.println("Found " + exportTariffs.size() + " export tariff rates for " + normalizedCountryCode);

        // Generate top import categories (highest tariff rates TO this country)
        dto.topImports = generateTopProducts(importTariffs);
        
        // Generate top export categories (tariff rates FROM this country)  
        dto.topExports = generateTopProducts(exportTariffs);

        // Generate major trading partners (countries with most tariff relationships)
        dto.majorPartners = generateTradingPartners(importTariffs, exportTariffs);

        // Calculate average tariffs
        dto.averageImportTariff = computeAverageTariff(importTariffs);
        dto.averageExportTariff = computeAverageTariff(exportTariffs);
        
        System.out.println("Average import tariff: " + dto.averageImportTariff);
        System.out.println("Average export tariff: " + dto.averageExportTariff);
        System.out.println("Trade insights generation completed for: " + normalizedCountryCode);

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
        // Group by product category and calculate metrics based on tariff rates
        Map<String, List<TariffRate>> productGroups = tariffRates.stream()
                .collect(Collectors.groupingBy(t -> t.getProductCategory().getCode()));

        return productGroups.entrySet().stream()
                .map(entry -> {
                    String productCode = entry.getKey();
                    List<TariffRate> rates = entry.getValue();
                    
                    ProductMetricDto dto = new ProductMetricDto();
                    dto.code = productCode;
                    dto.name = rates.get(0).getProductCategory().getName();
                    
                    // Use tariff count and average rate to create a significance score
                    BigDecimal avgRate = rates.stream()
                            .map(TariffRate::getBaseRate)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(rates.size()), 4, RoundingMode.HALF_UP);
                    
                    // Significance = (average_rate * rate_count * base_multiplier)
                    BigDecimal significance = avgRate
                            .multiply(BigDecimal.valueOf(rates.size()))
                            .multiply(BigDecimal.valueOf(5000000)); // Base multiplier for display
                    
                    dto.totalValue = significance;
                    return dto;
                })
                .sorted((a, b) -> b.totalValue.compareTo(a.totalValue))
                .limit(MAX_ITEMS)
                .collect(Collectors.toList());
    }

    private List<PartnerMetricDto> generateTradingPartners(List<TariffRate> importTariffs, 
                                                          List<TariffRate> exportTariffs) {
        Map<String, PartnerMetricDto> partnerMap = new HashMap<>();
        
        // Count import partners (countries that export TO the selected country)
        Map<String, List<TariffRate>> importPartners = importTariffs.stream()
                .collect(Collectors.groupingBy(t -> t.getOrigin().getCode()));
        
        for (Map.Entry<String, List<TariffRate>> entry : importPartners.entrySet()) {
            String code = entry.getKey();
            List<TariffRate> rates = entry.getValue();
            
            PartnerMetricDto dto = new PartnerMetricDto();
            dto.code = code;
            dto.name = rates.get(0).getOrigin().getName();
            dto.totalValue = BigDecimal.valueOf(rates.size()).multiply(BigDecimal.valueOf(2000000));
            partnerMap.put(code, dto);
        }

        // Add export partners (countries that import FROM the selected country)
        Map<String, List<TariffRate>> exportPartners = exportTariffs.stream()
                .collect(Collectors.groupingBy(t -> t.getDestination().getCode()));
        
        for (Map.Entry<String, List<TariffRate>> entry : exportPartners.entrySet()) {
            String code = entry.getKey();
            List<TariffRate> rates = entry.getValue();
            
            PartnerMetricDto existing = partnerMap.get(code);
            if (existing != null) {
                // Add export relationship strength
                existing.totalValue = existing.totalValue.add(
                    BigDecimal.valueOf(rates.size()).multiply(BigDecimal.valueOf(2000000))
                );
            } else {
                PartnerMetricDto dto = new PartnerMetricDto();
                dto.code = code;
                dto.name = rates.get(0).getDestination().getName();
                dto.totalValue = BigDecimal.valueOf(rates.size()).multiply(BigDecimal.valueOf(2000000));
                partnerMap.put(code, dto);
            }
        }

        return partnerMap.values().stream()
                .sorted((a, b) -> b.totalValue.compareTo(a.totalValue))
                .limit(MAX_ITEMS)
                .collect(Collectors.toList());
    }
}
