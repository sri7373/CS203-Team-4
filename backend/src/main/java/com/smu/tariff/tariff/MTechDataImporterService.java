package com.smu.tariff.tariff;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.repository.TariffRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MTechDataImporterService {

    private static final Logger log = LoggerFactory.getLogger(MTechDataImporterService.class);

    private final ProductCategoryRepository productCategoryRepository;
    private final TariffRateRepository tariffRateRepository;
    private final CountryRepository countryRepository;
    private final RestTemplate restTemplate;

    private final String TOKEN = "r21Ko8rKk8T8MtD8j5lk82A66hb0w2NF"; // Latest token with quota

    public MTechDataImporterService(ProductCategoryRepository productCategoryRepository,
                                    TariffRateRepository tariffRateRepository,
                                    CountryRepository countryRepository,
                                    RestTemplate restTemplate) {
        this.productCategoryRepository = productCategoryRepository;
        this.tariffRateRepository = tariffRateRepository;
        this.countryRepository = countryRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void importData() {
        log.info("Starting MTech data import...");
        
        // Fixed list of product categories
        List<ProductCategory> categories = productCategoryRepository.findAll();
        log.info("Found {} product categories", categories.size());

        if (categories.isEmpty()) {
            log.warn("No product categories found in database. Please ensure categories are populated first.");
            return;
        }

        // First, set HS codes for all categories (in one transaction)
        for (ProductCategory category : categories) {
            log.info("Processing category: {} ({})", category.getName(), category.getCode());
            
            // Only check weight-based for alcohol and tobacco
            boolean weightBased = category.getCode().equalsIgnoreCase("ALCOHOL") ||
                                  category.getCode().equalsIgnoreCase("TOBACCO");
            category.setWeightBased(weightBased);

            // Set proper HS codes based on category
            String hsCode;
            switch (category.getCode().toUpperCase()) {
                case "FOOD":
                    hsCode = "210690"; // Food preparations
                    break;
                case "STEEL":
                    hsCode = "720851"; // Steel sheets
                    break;
                case "ELEC":
                case "ELECTRONICS":
                    hsCode = "850440"; // Electronic equipment
                    break;
                case "TOBACCO":
                    hsCode = "240110"; // Tobacco leaves
                    break;
                case "ALCOHOL":
                    hsCode = "220830"; // Whisky
                    break;
                default:
                    hsCode = "640411"; // Default (footwear example from your API)
                    break;
            }
            
            category.setHsCode(hsCode);
            log.info("✅ Set HS code {} for category {}", hsCode, category.getCode());
            productCategoryRepository.save(category);
        }

        // Now import tariff rates (each will be committed separately)
        importTariffRatesInSeparateTransactions(categories);
        log.info("MTech data import completed successfully!");
    }

    // Import tariff rates with individual transactions for real-time visibility
    public void importTariffRatesInSeparateTransactions(List<ProductCategory> categories) {
        log.info("Starting tariff rate population...");
        List<Country> allCountries = countryRepository.findAll();
        log.info("Found {} countries for tariff rate generation", allCountries.size());

        int totalPairs = categories.size() * (allCountries.size() * (allCountries.size() - 1));
        int currentPair = 0;

        for (ProductCategory category : categories) {
            if (category.getHsCode() == null) {
                log.warn("Skipping category {} - no HS code found", category.getCode());
                continue;
            }

            // Generate tariff rates for all country pairs
            for (Country originCountry : allCountries) {
                for (Country destinationCountry : allCountries) {
                    // Skip if origin and destination are the same
                    if (originCountry.getId().equals(destinationCountry.getId())) {
                        continue;
                    }

                    currentPair++;
                    log.info("Progress: {}/{} - Processing {} → {} for {}", 
                            currentPair, totalPairs, originCountry.getCode(), 
                            destinationCountry.getCode(), category.getCode());

                    // Import this single tariff rate in its own transaction
                    importSingleTariffRate(category, originCountry, destinationCountry);
                }
            }
        }
    }

    @Transactional
    public void importSingleTariffRate(ProductCategory category, Country originCountry, Country destinationCountry) {
        String originCode = getNumericCountryCode(originCountry.getCode());
        String destinationCode = getNumericCountryCode(destinationCountry.getCode());

        if (originCode == null || destinationCode == null) {
            log.warn("Skipping {} → {} - unsupported country codes", 
                    originCountry.getCode(), destinationCountry.getCode());
            return;
        }

        String importDutyUrl = String.format(
                "https://mtech-api.com/client/api/tariff-data?hscode=%s&country=%s&token=%s",
                category.getHsCode(), destinationCode, TOKEN
        );

        try {
            log.info("Fetching duty rates for HS code {} from {} ({}) to {} ({})", 
                    category.getHsCode(), originCountry.getCode(), originCode, 
                    destinationCountry.getCode(), destinationCode);
            
            // Log the exact URL being called
            log.info("🔗 API URL: {}", importDutyUrl);
            
            ImportDutyResponse dutyResponse = restTemplate.getForObject(importDutyUrl, ImportDutyResponse.class);
            
            // Detailed logging of the raw response
            if (dutyResponse == null) {
                log.error("❌ NULL response from MTech API");
            } else {
                log.info("✅ Raw API Response - Base: {}, Fee: {}", 
                        dutyResponse.getBaseRate(), dutyResponse.getAdditionalFee());
                
                // Check if we're getting the expected data structure
                if (dutyResponse.getBaseRate() == 0.0 && dutyResponse.getAdditionalFee() == 0.0) {
                    log.warn("⚠️ Both base rate and fee are 0.0 - possible API issue or free trade");
                }
            }
            
            log.info("MTECH DUTY RESPONSE for HS code {} ({} → {}): {}", 
                    category.getHsCode(), originCode, destinationCode,
                    (dutyResponse != null ? "Base=" + dutyResponse.getBaseRate() + ", Fee=" + dutyResponse.getAdditionalFee() : "null"));

            if (dutyResponse == null) {
                log.warn("No duty response for HS code {} ({} → {})", category.getHsCode(), originCode, destinationCode);
                return;
            }

            TariffRate rate = new TariffRate();
            rate.setProductCategory(category);
            rate.setOrigin(originCountry);
            rate.setDestination(destinationCountry);
            rate.setBaseRate(BigDecimal.valueOf(dutyResponse.getBaseRate()));
            rate.setAdditionalFee(BigDecimal.valueOf(dutyResponse.getAdditionalFee()));
            rate.setEffectiveFrom(LocalDate.now());

            TariffRate saved = tariffRateRepository.save(rate);
            log.info("✅ Saved tariff rate with ID {} for category {} ({} -> {}) - COMMITTED TO DB", 
                    saved.getId(), category.getCode(), originCode, destinationCode);
        } catch (Exception e) {
            log.error("Error fetching/saving duty for HS code {} ({} -> {}): {}", 
                    category.getHsCode(), originCode, destinationCode, e.getMessage(), e);
        }
    }

    // Helper method to convert text country codes to numeric codes for MTech API
    private String getNumericCountryCode(String textCode) {
        switch (textCode.toUpperCase()) {
            case "SGP":
                return "702"; // Singapore
            case "CHN":
                return "156"; // China
            case "USA":
                return "840"; // United States
            case "MYS":
                return "458"; // Malaysia
            case "IDN":
                return "360"; // Indonesia
            default:
                return null; // Unsupported country
        }
    }

    // New method: Import FTA data with non-zero rates only
    public String importFtaData() {
        log.info("🚀 Starting FTA data import (non-zero rates only)...");
        StringBuilder result = new StringBuilder();
        
        try {
            List<ProductCategory> categories = productCategoryRepository.findAll();
            log.info("Found {} categories", categories.size());
            
            if (categories.isEmpty()) {
                String msg = "❌ No product categories found!";
                log.error(msg);
                return msg;
            }
            
            result.append("🔄 Importing FTA data from /import-duty API\n");
            result.append("Priority: CPTPP agreements, Preferential rates\n");
            result.append("Filter: Non-zero rates only\n\n");
            
            int totalImported = 0;
            int skippedZero = 0;
            int skippedNoData = 0;
            int apiCalls = 0;
            
            // Test combinations strategically - countries with known FTA relationships
            String[] testOrigins = {"SGP", "MYS", "USA", "CHN", "IDN"};
            String[] testDestinations = {"JPN", "AUS", "CAN", "NZL", "CHL", "MEX", "PER", "VNM", "USA", "SGP", "MYS"};
            
            log.info("Testing {} origins x {} destinations x {} categories = {} combinations",
                testOrigins.length, testDestinations.length, categories.size(),
                testOrigins.length * testDestinations.length * categories.size());
            
            for (ProductCategory category : categories) {
                String hsCode = category.getHsCode();
                if (hsCode == null || hsCode.length() < 6) {
                    log.warn("Skipping category {} - invalid HS code", category.getCode());
                    continue;
                }
                
                result.append(String.format("📦 Category: %s (HS: %s)\n", category.getName(), hsCode));
                log.info("Processing category: {} ({})", category.getName(), hsCode);
                
                for (String originCode : testOrigins) {
                    for (String destCode : testDestinations) {
                        try {
                            apiCalls++;
                            log.info("API Call #{}: {} -> {}, HS: {}", apiCalls, originCode, destCode, hsCode);
                        String originNumeric = getNumericCountryCode(originCode);
                        String destNumeric = getNumericCountryCode(destCode);
                        
                        if (originNumeric == null || destNumeric == null) {
                            // Try to add new country
                            if (originNumeric == null) originNumeric = addCountryIfNeeded(originCode);
                            if (destNumeric == null) destNumeric = addCountryIfNeeded(destCode);
                        }
                        
                        String url = String.format(
                            "https://mtech-api.com/client/api/import-duty?hscode=%s&origin_country=%s&dest_country=%s&token=%s",
                            hsCode, originNumeric, destNumeric, TOKEN
                        );
                        
                        log.info("Calling FTA API: {} -> {}, HS: {}", originCode, destCode, hsCode);
                        
                        FtaResponse ftaResponse = restTemplate.getForObject(url, FtaResponse.class);
                        
                        if (ftaResponse != null && ftaResponse.getFTAImportDuty() != null) {
                            boolean imported = processFtaResponse(ftaResponse, category, originCode, destCode, result);
                            if (imported) {
                                totalImported++;
                            } else {
                                skippedZero++;
                            }
                        } else {
                            skippedNoData++;
                        }
                        
                        // Sleep to avoid rate limiting
                        Thread.sleep(500);
                        
                    } catch (org.springframework.web.client.HttpClientErrorException e) {
                        if (e.getStatusCode().value() == 429) {
                            log.error("❌ API QUOTA EXCEEDED! Stopping import.");
                            result.append(String.format("\n❌ API quota exceeded at call #%d\n", apiCalls));
                            result.append("Please use new token and try again!\n");
                            return result.toString();
                        }
                        log.error("Error importing FTA data for {} -> {}: {}", originCode, destCode, e.getMessage());
                        skippedNoData++;
                    } catch (Exception e) {
                        log.error("Error importing FTA data for {} -> {}: {}", originCode, destCode, e.getMessage());
                        skippedNoData++;
                    }
                }
            }
        }
        
        result.append(String.format("\n✅ Import complete!\n"));
        result.append(String.format("Imported: %d non-zero rates\n", totalImported));
        result.append(String.format("Skipped (zero/free): %d\n", skippedZero));
        result.append(String.format("Skipped (no data): %d\n", skippedNoData));
        result.append(String.format("Total API calls: %d\n", apiCalls));
        
        log.info("FTA import complete: {} imported, {} skipped zero, {} no data, {} API calls",
            totalImported, skippedZero, skippedNoData, apiCalls);
        
        return result.toString();
            
        } catch (Exception e) {
            log.error("❌ Fatal error in FTA import: {}", e.getMessage(), e);
            return "❌ Fatal error: " + e.getMessage() + "\nCheck logs for details.";
        }
    }
    
    private boolean processFtaResponse(FtaResponse response, ProductCategory category, 
                                      String originCode, String destCode, StringBuilder result) {
        try {
            // Get origin and destination countries
            Country origin = countryRepository.findByCode(originCode).orElse(null);
            Country dest = countryRepository.findByCode(destCode).orElse(null);
            
            if (origin == null || dest == null) {
                return false;
            }
            
            // Try ALL FTA agreements, not just CPTPP
            for (List<FtaAgreement> agreementList : response.getFTAImportDuty()) {
                for (FtaAgreement agreement : agreementList) {
                    if (agreement.getFTAName() != null) {
                        // Try to get preferential rate first
                        Double rate = extractPreferentialRate(agreement);
                        
                        // If preferential is 0% or null, try MFN rate as fallback
                        if (rate == null || rate == 0) {
                            rate = extractMfnRate(agreement);
                        }
                        
                        if (rate != null && rate > 0) {
                            LocalDate effectiveDate = parseInforceDate(agreement.getAgreementData());
                            
                            // Check if already exists
                            if (tariffRateExists(origin, dest, category, effectiveDate)) {
                                log.info("⏭️  Skipping duplicate: {} -> {} (already exists)", originCode, destCode);
                                return false;
                            }
                            
                            TariffRate tariffRate = new TariffRate();
                            tariffRate.setOrigin(origin);
                            tariffRate.setDestination(dest);
                            tariffRate.setProductCategory(category);
                            tariffRate.setBaseRate(BigDecimal.valueOf(rate));
                            tariffRate.setAdditionalFee(BigDecimal.ZERO);
                            tariffRate.setEffectiveFrom(effectiveDate != null ? effectiveDate : LocalDate.now());
                            
                            tariffRateRepository.save(tariffRate);
                            
                            result.append(String.format("  ✅ %s → %s: %.1f%% (%s, since %s)\n", 
                                originCode, destCode, rate, agreement.getFTAName(), effectiveDate));
                            
                            log.info("✅ Imported: {} -> {} = {}% ({})", originCode, destCode, rate, agreement.getFTAName());
                            
                            return true; // Only save once per country pair
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("Error processing FTA response: {}", e.getMessage());
            return false;
        }
    }
    
    private Double extractPreferentialRate(FtaAgreement agreement) {
        if (agreement.getImportDutyData() != null) {
            for (ImportDutyInfo dutyInfo : agreement.getImportDutyData()) {
                if ("Preferential".equalsIgnoreCase(dutyInfo.getImportDutyType())) {
                    String rateStr = dutyInfo.getImportDutyRate();
                    if (rateStr != null && !rateStr.equalsIgnoreCase("Free") && !rateStr.equals("0%")) {
                        try {
                            return Double.parseDouble(rateStr.replace("%", "").trim());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private Double extractMfnRate(FtaAgreement agreement) {
        if (agreement.getImportDutyData() != null) {
            for (ImportDutyInfo dutyInfo : agreement.getImportDutyData()) {
                if ("MFN".equalsIgnoreCase(dutyInfo.getImportDutyType())) {
                    String rateStr = dutyInfo.getImportDutyRate();
                    if (rateStr != null && !rateStr.equalsIgnoreCase("Free") && !rateStr.equals("0%")) {
                        try {
                            return Double.parseDouble(rateStr.replace("%", "").trim());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private LocalDate parseInforceDate(AgreementInfo agreementInfo) {
        if (agreementInfo == null || agreementInfo.getInforceDate() == null) {
            return null;
        }
        
        try {
            String dateStr = agreementInfo.getInforceDate();
            // Parse "30.12.2018" format
            String[] parts = dateStr.split("\\.");
            if (parts.length == 3) {
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            log.warn("Could not parse date: {}", agreementInfo.getInforceDate());
        }
        
        return null;
    }
    
    private String addCountryIfNeeded(String code) {
        // Map common country codes to numeric
        String numeric = getNumericCountryCode(code);
        if (numeric != null) return numeric;
        
        // Try to add new countries dynamically
        switch (code.toUpperCase()) {
            case "JPN": return addCountry("JPN", "Japan", "392");
            case "AUS": return addCountry("AUS", "Australia", "036");
            case "CAN": return addCountry("CAN", "Canada", "124");
            case "NZL": return addCountry("NZL", "New Zealand", "554");
            case "CHL": return addCountry("CHL", "Chile", "152");
            case "MEX": return addCountry("MEX", "Mexico", "484");
            case "PER": return addCountry("PER", "Peru", "604");
            case "VNM": return addCountry("VNM", "Vietnam", "704");
            default:
                log.warn("Unknown country code: {}", code);
                return null;
        }
    }
    
    private String addCountry(String code, String name, String numeric) {
        try {
            if (countryRepository.findByCode(code).isEmpty()) {
                Country country = new Country();
                country.setCode(code);
                country.setName(name);
                countryRepository.save(country);
                log.info("✅ Added new country: {} ({})", name, code);
            }
            return numeric;
        } catch (Exception e) {
            log.error("Failed to add country {}: {}", code, e.getMessage());
            return null;
        }
    }
    
    // Hybrid import: FTA + Customs Authority data for comprehensive coverage
    public String importHybridData() {
        log.info("🚀 Starting Hybrid data import (FTA + Customs Authority)...");
        StringBuilder result = new StringBuilder();
        
        try {
            List<ProductCategory> categories = productCategoryRepository.findAll();
            log.info("Found {} categories", categories.size());
            
            if (categories.isEmpty()) {
                return "❌ No product categories found!";
            }
            
            result.append("🔄 Hybrid Import Strategy:\n");
            result.append("1. FTA routes (ASEAN, CPTPP, etc.) → /import-duty API\n");
            result.append("2. Non-FTA routes (USA, etc.) → /tariff-data API\n");
            result.append("Filter: Non-zero rates only\n\n");
            
            int totalImported = 0;
            int skippedZero = 0;
            int skippedNoData = 0;
            int skippedDuplicates = 0;
            int apiCalls = 0;
            
            // FTA-heavy routes (ASEAN countries to FTA partners)
            String[] ftaOrigins = {"SGP", "MYS", "IDN"};
            String[] ftaDestinations = {"JPN", "AUS", "CAN", "NZL", "CHL", "MEX", "PER", "VNM"};
            
            // Non-FTA routes (to USA and China)
            String[] customsOrigins = {"SGP", "MYS", "IDN", "CHN"};
            String[] customsDestinations = {"USA", "CHN"};
            
            // Phase 1: Import FTA data
            result.append("📦 Phase 1: FTA Routes\n");
            for (ProductCategory category : categories) {
                String hsCode = category.getHsCode();
                if (hsCode == null || hsCode.length() < 6) continue;
                
                for (String origin : ftaOrigins) {
                    for (String dest : ftaDestinations) {
                        try {
                            apiCalls++;
                            log.info("API Call #{}: {} -> {} for {}", apiCalls, origin, dest, category.getCode());
                            if (importFtaRoute(origin, dest, hsCode, category, result)) {
                                totalImported++;
                            } else {
                                skippedZero++;
                            }
                            Thread.sleep(500);
                        } catch (org.springframework.dao.DataIntegrityViolationException e) {
                            // Duplicate - database constraint caught it
                            skippedDuplicates++;
                            log.debug("Duplicate caught by database: {} -> {}", origin, dest);
                        } catch (org.springframework.web.client.HttpClientErrorException e) {
                            if (e.getStatusCode().value() == 429) {
                                result.append("\n❌ API quota exceeded!\n");
                                return finalizeResultWithDuplicates(result, totalImported, skippedZero, skippedNoData, skippedDuplicates, apiCalls);
                            }
                            skippedNoData++;
                        } catch (Exception e) {
                            log.error("Error: {}", e.getMessage());
                            skippedNoData++;
                        }
                    }
                }
            }
            
            // Phase 2: Import Customs Authority data for non-FTA routes
            result.append("\n📦 Phase 2: Customs Authority Routes (USA, CHN)\n");
            for (ProductCategory category : categories) {
                String hsCode = category.getHsCode();
                if (hsCode == null || hsCode.length() < 6) continue;
                
                for (String dest : customsDestinations) {
                    try {
                        apiCalls++;
                        log.info("API Call #{}: Customs route to {} for {}", apiCalls, dest, category.getCode());
                        int imported = importCustomsRouteSimple(dest, hsCode, category, customsOrigins, result);
                        if (imported > 0) {
                            totalImported += imported;
                        } else {
                            skippedZero++;
                        }
                        Thread.sleep(500);
                    } catch (org.springframework.web.client.HttpClientErrorException e) {
                        if (e.getStatusCode().value() == 429) {
                            result.append("\n❌ API quota exceeded!\n");
                            return finalizeResultWithDuplicates(result, totalImported, skippedZero, skippedNoData, skippedDuplicates, apiCalls);
                        }
                        skippedNoData++;
                    } catch (Exception e) {
                        log.error("Error: {}", e.getMessage());
                        skippedNoData++;
                    }
                }
            }
            
            return finalizeResultWithDuplicates(result, totalImported, skippedZero, skippedNoData, skippedDuplicates, apiCalls);
            
        } catch (Exception e) {
            log.error("❌ Fatal error: {}", e.getMessage(), e);
            return "❌ Fatal error: " + e.getMessage();
        }
    }
    
    private boolean importFtaRoute(String origin, String dest, String hsCode, 
                                   ProductCategory category, StringBuilder result) {
        try {
            String originNumeric = getNumericCountryCode(origin);
            String destNumeric = getNumericCountryCode(dest);
            
            if (originNumeric == null) originNumeric = addCountryIfNeeded(origin);
            if (destNumeric == null) destNumeric = addCountryIfNeeded(dest);
            if (originNumeric == null || destNumeric == null) return false;
            
            String url = String.format(
                "https://mtech-api.com/client/api/import-duty?hscode=%s&origin_country=%s&dest_country=%s&token=%s",
                hsCode, originNumeric, destNumeric, TOKEN
            );
            
            log.info("Calling FTA API: {} -> {}", origin, dest);
            FtaResponse ftaResponse = null;
            try {
                ftaResponse = restTemplate.getForObject(url, FtaResponse.class);
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.warn("Timeout or connection error for {} -> {}: {}", origin, dest, e.getMessage());
                return false;
            }
            
            if (ftaResponse != null && ftaResponse.getFTAImportDuty() != null) {
                return processFtaResponse(ftaResponse, category, origin, dest, result);
            }
            return false;
        } catch (Exception e) {
            log.error("Error importing FTA route {} -> {}: {}", origin, dest, e.getMessage());
            return false;
        }
    }
    
    private int importCustomsRoute(String dest, String hsCode, 
                                    ProductCategory category, String[] originsToCheck,
                                    Set<String> existingCombinations, StringBuilder result) {
        try {
            String destNumeric = getNumericCountryCode(dest);
            if (destNumeric == null) return 0;
            
            String url = String.format(
                "https://mtech-api.com/client/api/tariff-data?hscode=%s&country=%s&token=%s",
                hsCode, destNumeric, TOKEN
            );
            
            ImportDutyResponse response = restTemplate.getForObject(url, ImportDutyResponse.class);
            
            if (response != null && response.getData() != null) {
                double rate = response.getBaseRate();
                if (rate > 0) {
                    // Save for multiple origins
                    int savedCount = 0;
                    
                    for (String originCode : originsToCheck) {
                        if (originCode.equals(dest)) continue; // Skip same origin-dest
                        
                        // Check if this combination was already processed
                        String key = String.format("%s-%s-%s", originCode, dest, category.getCode());
                        if (existingCombinations.contains(key)) {
                            continue; // Skip duplicate
                        }
                        
                        Country origin = countryRepository.findByCode(originCode).orElse(null);
                        Country destination = countryRepository.findByCode(dest).orElse(null);
                        
                        if (origin != null && destination != null) {
                            TariffRate tariffRate = new TariffRate();
                            tariffRate.setOrigin(origin);
                            tariffRate.setDestination(destination);
                            tariffRate.setProductCategory(category);
                            tariffRate.setBaseRate(BigDecimal.valueOf(rate));
                            tariffRate.setAdditionalFee(BigDecimal.valueOf(response.getAdditionalFee()));
                            tariffRate.setEffectiveFrom(LocalDate.now());
                            
                            tariffRateRepository.save(tariffRate);
                            existingCombinations.add(key); // Track newly added
                            savedCount++;
                            
                            if (savedCount == 1) {
                                result.append(String.format("  ✅ Multiple → %s: %.1f%% (Customs Authority)\n", 
                                    dest, rate));
                            }
                            
                            log.info("✅ Imported: {} -> {} = {}% (Customs)", originCode, dest, rate);
                        }
                    }
                    return savedCount;
                }
            }
            return 0;
        } catch (Exception e) {
            throw e;
        }
    }
    
    private int importCustomsRouteSimple(String dest, String hsCode, 
                                         ProductCategory category, String[] originsToCheck,
                                         StringBuilder result) {
        try {
            String destNumeric = getNumericCountryCode(dest);
            if (destNumeric == null) return 0;
            
            String url = String.format(
                "https://mtech-api.com/client/api/tariff-data?hscode=%s&country=%s&token=%s",
                hsCode, destNumeric, TOKEN
            );
            
            ImportDutyResponse response = restTemplate.getForObject(url, ImportDutyResponse.class);
            
            if (response != null && response.getData() != null) {
                double rate = response.getBaseRate();
                if (rate > 0) {
                    int savedCount = 0;
                    
                    for (String originCode : originsToCheck) {
                        if (originCode.equals(dest)) continue;
                        
                        Country origin = countryRepository.findByCode(originCode).orElse(null);
                        Country destination = countryRepository.findByCode(dest).orElse(null);
                        
                        if (origin != null && destination != null) {
                            try {
                                TariffRate tariffRate = new TariffRate();
                                tariffRate.setOrigin(origin);
                                tariffRate.setDestination(destination);
                                tariffRate.setProductCategory(category);
                                tariffRate.setBaseRate(BigDecimal.valueOf(rate));
                                tariffRate.setAdditionalFee(BigDecimal.valueOf(response.getAdditionalFee()));
                                tariffRate.setEffectiveFrom(LocalDate.now());
                                
                                tariffRateRepository.save(tariffRate);
                                savedCount++;
                                
                                if (savedCount == 1) {
                                    result.append(String.format("  ✅ Multiple → %s: %.1f%% (Customs)\n", dest, rate));
                                }
                                
                                log.info("✅ Imported: {} -> {} = {}% (Customs)", originCode, dest, rate);
                            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                                // Duplicate - skip silently
                                log.debug("Duplicate: {} -> {}", originCode, dest);
                            }
                        }
                    }
                    return savedCount;
                }
            }
            return 0;
        } catch (Exception e) {
            throw e;
        }
    }
    
    private String finalizeResultWithDuplicates(StringBuilder result, int totalImported, 
                                                int skippedZero, int skippedNoData, 
                                                int skippedDuplicates, int apiCalls) {
        result.append(String.format("\n✅ Import complete!\n"));
        result.append(String.format("Imported: %d non-zero rates\n", totalImported));
        result.append(String.format("Skipped (duplicates): %d\n", skippedDuplicates));
        result.append(String.format("Skipped (zero/free): %d\n", skippedZero));
        result.append(String.format("Skipped (no data): %d\n", skippedNoData));
        result.append(String.format("Total API calls: %d\n", apiCalls));
        
        log.info("Hybrid import complete: {} imported, {} skipped duplicates, {} API calls", 
                totalImported, skippedDuplicates, apiCalls);
        return result.toString();
    }
    
    private String finalizeResult(StringBuilder result, int totalImported, 
                                  int skippedZero, int skippedNoData, int apiCalls) {
        result.append(String.format("\n✅ Import complete!\n"));
        result.append(String.format("Imported: %d non-zero rates\n", totalImported));
        result.append(String.format("Skipped (zero/free): %d\n", skippedZero));
        result.append(String.format("Skipped (no data): %d\n", skippedNoData));
        result.append(String.format("Total API calls: %d\n", apiCalls));
        
        log.info("Hybrid import complete: {} imported, {} API calls", totalImported, apiCalls);
        return result.toString();
    }
    
    private boolean tariffRateExists(Country origin, Country dest, ProductCategory category, LocalDate effectiveDate) {
        LocalDate dateToCheck = effectiveDate != null ? effectiveDate : LocalDate.now();
        return tariffRateRepository.existsByOriginAndDestinationAndProductCategoryAndEffectiveFrom(
            origin, dest, category, dateToCheck
        );
    }

    // POJOs for JSON mapping - Updated to match actual MTech API structure
    public static class HsCodeResponse {
        private List<HsCodeData> data;
        public List<HsCodeData> getData() { return data; }
        public void setData(List<HsCodeData> data) { this.data = data; }
    }

    public static class HsCodeData {
        private String hsCode;
        public String getHsCode() { return hsCode; }
        public void setHsCode(String hsCode) { this.hsCode = hsCode; }
    }

    // Structure matching MTech API Customs Authority response
    // Note: API returns different structures - sometimes array, sometimes single object
    public static class ImportDutyResponse {
        private Object data; // Can be List<LinkedHashMap> or LinkedHashMap
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        
        // Helper to get first tariff data with an actual duty rate (not "Free")
        private TariffData getFirstTariffData() {
            if (data == null) return null;
            
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                // Search for first item with actual duty rate (not "Free")
                for (Object item : list) {
                    if (item instanceof java.util.Map) {
                        TariffData tariffData = convertMapToTariffData((java.util.Map<?, ?>) item);
                        if (tariffData != null && tariffData.getTariffRate() != null) {
                            String generalRate = tariffData.getTariffRate().getGeneralRateOfDuty();
                            // Skip "Free" entries, look for actual rates
                            if (generalRate != null && !generalRate.equalsIgnoreCase("Free") 
                                && !generalRate.equalsIgnoreCase("null") && !generalRate.isEmpty()) {
                                return tariffData;
                            }
                        }
                    }
                }
                // If no rate found, return first item as fallback
                if (!list.isEmpty() && list.get(0) instanceof java.util.Map) {
                    return convertMapToTariffData((java.util.Map<?, ?>) list.get(0));
                }
            } else if (data instanceof java.util.Map) {
                return convertMapToTariffData((java.util.Map<?, ?>) data);
            }
            return null;
        }
        
        // Convert LinkedHashMap to TariffData
        private TariffData convertMapToTariffData(java.util.Map<?, ?> map) {
            TariffData tariffData = new TariffData();
            tariffData.setHsCode((String) map.get("hs_code"));
            tariffData.setDesc((String) map.get("desc"));
            
            // Handle tariff_rate object
            Object tariffRateObj = map.get("tariff_rate");
            if (tariffRateObj instanceof java.util.Map) {
                java.util.Map<?, ?> rateMap = (java.util.Map<?, ?>) tariffRateObj;
                TariffRateInfo rateInfo = new TariffRateInfo();
                rateInfo.setGeneralRateOfDuty((String) rateMap.get("General Rate of Duty"));
                rateInfo.setSpecialRateOfDuty((String) rateMap.get("Special Rate of Duty"));
                rateInfo.setAdditionalDuties((String) rateMap.get("Additional Duties"));
                tariffData.setTariffRate(rateInfo);
            }
            
            return tariffData;
        }
        
        // Helper method to extract duty rate from first result
        public double getBaseRate() {
            TariffData tariffData = getFirstTariffData();
            if (tariffData != null && tariffData.getTariffRate() != null) {
                String generalRate = tariffData.getTariffRate().getGeneralRateOfDuty();
                if (generalRate != null && !generalRate.isEmpty()) {
                    // Parse "8.5%" to 8.5
                    String rateStr = generalRate.replace("%", "").trim();
                    try {
                        return Double.parseDouble(rateStr);
                    } catch (NumberFormatException e) {
                        // Try to extract first percentage if format is complex like "Free" or "0.5¢/kg"
                        return 0.0;
                    }
                }
            }
            return 0.0;
        }
        
        public double getAdditionalFee() {
            TariffData tariffData = getFirstTariffData();
            if (tariffData != null && tariffData.getTariffRate() != null) {
                String additionalDuties = tariffData.getTariffRate().getAdditionalDuties();
                if (additionalDuties != null && !additionalDuties.isEmpty() && !additionalDuties.equalsIgnoreCase("null")) {
                    // Try to parse additional duties if present
                    try {
                        return Double.parseDouble(additionalDuties);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                }
            }
            return 0.0;
        }
    }
    
    public static class TariffData {
        @JsonProperty("hs_code")
        private String hsCode;
        
        @JsonProperty("tariff_rate")
        private TariffRateInfo tariffRate;
        
        @JsonProperty("desc")
        private String desc;
        
        public String getHsCode() { return hsCode; }
        public void setHsCode(String hsCode) { this.hsCode = hsCode; }
        public TariffRateInfo getTariffRate() { return tariffRate; }
        public void setTariffRate(TariffRateInfo tariffRate) { this.tariffRate = tariffRate; }
        public String getDesc() { return desc; }
        public void setDesc(String desc) { this.desc = desc; }
    }
    
    public static class TariffRateInfo {
        @JsonProperty("General Rate of Duty")
        private String generalRateOfDuty;
        
        @JsonProperty("Special Rate of Duty")
        private String specialRateOfDuty;
        
        @JsonProperty("Additional Duties")
        private String additionalDuties;
        
        public String getGeneralRateOfDuty() { return generalRateOfDuty; }
        public void setGeneralRateOfDuty(String generalRateOfDuty) { this.generalRateOfDuty = generalRateOfDuty; }
        
        public String getSpecialRateOfDuty() { return specialRateOfDuty; }
        public void setSpecialRateOfDuty(String specialRateOfDuty) { this.specialRateOfDuty = specialRateOfDuty; }
        
        public String getAdditionalDuties() { return additionalDuties; }
        public void setAdditionalDuties(String additionalDuties) { this.additionalDuties = additionalDuties; }
    }
    
    // FTA API Response POJOs
    public static class FtaResponse {
        @JsonProperty("FTAImportDuty")
        private List<List<FtaAgreement>> FTAImportDuty;
        
        public List<List<FtaAgreement>> getFTAImportDuty() { return FTAImportDuty; }
        public void setFTAImportDuty(List<List<FtaAgreement>> FTAImportDuty) { this.FTAImportDuty = FTAImportDuty; }
    }
    
    public static class FtaAgreement {
        @JsonProperty("FTAName")
        private String FTAName;
        
        @JsonProperty("ImportDutyData")
        private List<ImportDutyInfo> ImportDutyData;
        
        @JsonProperty("AgreementData")
        private AgreementInfo AgreementData;
        
        public String getFTAName() { return FTAName; }
        public void setFTAName(String FTAName) { this.FTAName = FTAName; }
        
        public List<ImportDutyInfo> getImportDutyData() { return ImportDutyData; }
        public void setImportDutyData(List<ImportDutyInfo> ImportDutyData) { this.ImportDutyData = ImportDutyData; }
        
        public AgreementInfo getAgreementData() { return AgreementData; }
        public void setAgreementData(AgreementInfo AgreementData) { this.AgreementData = AgreementData; }
    }
    
    public static class ImportDutyInfo {
        @JsonProperty("ImportDutyRate")
        private String ImportDutyRate;
        
        @JsonProperty("ImportDutyType")
        private String ImportDutyType;
        
        public String getImportDutyRate() { return ImportDutyRate; }
        public void setImportDutyRate(String ImportDutyRate) { this.ImportDutyRate = ImportDutyRate; }
        
        public String getImportDutyType() { return ImportDutyType; }
        public void setImportDutyType(String ImportDutyType) { this.ImportDutyType = ImportDutyType; }
    }
    
    public static class AgreementInfo {
        @JsonProperty("InforceDate")
        private String InforceDate;
        
        @JsonProperty("SignedDate")
        private String SignedDate;
        
        @JsonProperty("Type")
        private String Type;
        
        public String getInforceDate() { return InforceDate; }
        public void setInforceDate(String InforceDate) { this.InforceDate = InforceDate; }
        
        public String getSignedDate() { return SignedDate; }
        public void setSignedDate(String SignedDate) { this.SignedDate = SignedDate; }
        
        public String getType() { return Type; }
        public void setType(String Type) { this.Type = Type; }
    }
}
