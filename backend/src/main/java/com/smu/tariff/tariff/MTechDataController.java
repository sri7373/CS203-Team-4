package com.smu.tariff.tariff;

import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.repository.TariffRateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@RestController
public class MTechDataController {

    private final MTechDataImporterService importerService;
    private final ProductCategoryRepository productCategoryRepository;
    private final CountryRepository countryRepository;
    private final TariffRateRepository tariffRateRepository;

    // Manual constructor (no Lombok)
    public MTechDataController(MTechDataImporterService importerService,
                              ProductCategoryRepository productCategoryRepository,
                              CountryRepository countryRepository,
                              TariffRateRepository tariffRateRepository) {
        this.importerService = importerService;
        this.productCategoryRepository = productCategoryRepository;
        this.countryRepository = countryRepository;
        this.tariffRateRepository = tariffRateRepository;
    }

    @GetMapping("/import-mtech-data")
    public ResponseEntity<String> importData() {
        try {
            importerService.importData();
            return ResponseEntity.ok("✅ MTech data imported successfully!");
        } catch (Exception e) { // catch all runtime exceptions safely
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to import MTech data: " + e.getMessage());
        }
    }
    
    @GetMapping("/import-fta-data")
    public ResponseEntity<String> importFtaData() {
        try {
            String result = importerService.importFtaData();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to import FTA data: " + e.getMessage());
        }
    }
    
    @GetMapping("/import-hybrid-data")
    public ResponseEntity<String> importHybridData() {
        try {
            String result = importerService.importHybridData();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to import hybrid data: " + e.getMessage());
        }
    }
    
    @GetMapping("/test-fta-api")
    public ResponseEntity<String> testFtaApi(
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam String hsCode) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            String originNumeric = getNumericCountryCode(origin);
            String destNumeric = getNumericCountryCode(dest);
            
            String url = String.format(
                "https://mtech-api.com/client/api/import-duty?hscode=%s&origin_country=%s&dest_country=%s&token=%s",
                hsCode, originNumeric, destNumeric, "XM7cLeePrzNWsxLuTmBPkT7nskV9bl7p"
            );
            
            StringBuilder result = new StringBuilder();
            result.append("🧪 Testing FTA API Call:\n\n");
            result.append("Origin: ").append(origin).append(" → ").append(originNumeric).append("\n");
            result.append("Destination: ").append(dest).append(" → ").append(destNumeric).append("\n");
            result.append("HS Code: ").append(hsCode).append("\n\n");
            result.append("URL: ").append(url).append("\n\n");
            
            String rawResponse = restTemplate.getForObject(url, String.class);
            result.append("📡 Raw Response Length: ").append(rawResponse != null ? rawResponse.length() : 0).append(" characters\n");
            if (rawResponse != null && rawResponse.length() < 5000) {
                result.append("📡 Raw Response: ").append(rawResponse).append("\n\n");
            } else if (rawResponse != null) {
                result.append("📡 Raw Response (first 5000 chars): ").append(rawResponse.substring(0, 5000)).append("...\n\n");
            }
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ FTA API test failed: " + e.getMessage() + "\n" + e.getClass().getName());
        }
    }

    @GetMapping("/debug-database")
    public ResponseEntity<String> debugDatabase() {
        try {
            long categoryCount = productCategoryRepository.count();
            long countryCount = countryRepository.count();
            long tariffRateCount = tariffRateRepository.count();
            
            StringBuilder debug = new StringBuilder();
            debug.append("📊 Database Debug Info:\n");
            debug.append("Product Categories: ").append(categoryCount).append("\n");
            debug.append("Countries: ").append(countryCount).append("\n");
            debug.append("Tariff Rates: ").append(tariffRateCount).append("\n\n");
            
            if (categoryCount == 0) {
                debug.append("⚠️ No product categories found! This is why import isn't working.\n");
            } else {
                debug.append("\n📦 Product Categories:\n");
                productCategoryRepository.findAll().forEach(cat -> {
                    debug.append(String.format("  %d. %s (code: %s, hs: %s)\n", 
                            cat.getId(), cat.getName(), cat.getCode(), 
                            cat.getHsCode() != null ? cat.getHsCode() : "NULL"));
                });
            }
            
            if (countryCount == 0) {
                debug.append("⚠️ No countries found! Add countries first.\n");
            } else {
                boolean hasSGP = countryRepository.findByCode("SGP").isPresent();
                boolean hasMYS = countryRepository.findByCode("MYS").isPresent();
                boolean hasUSA = countryRepository.findByCode("USA").isPresent();
                boolean hasCHN = countryRepository.findByCode("CHN").isPresent();
                boolean hasIDN = countryRepository.findByCode("IDN").isPresent();
                debug.append("Singapore (SGP): ").append(hasSGP ? "✅" : "❌").append("\n");
                debug.append("Malaysia (MYS): ").append(hasMYS ? "✅" : "❌").append("\n");
                debug.append("USA (USA): ").append(hasUSA ? "✅" : "❌").append("\n");
                debug.append("China (CHN): ").append(hasCHN ? "✅" : "❌").append("\n");
                debug.append("Indonesia (IDN): ").append(hasIDN ? "✅" : "❌").append("\n");
            }
            
            return ResponseEntity.ok(debug.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Debug failed: " + e.getMessage());
        }
    }

    @GetMapping("/populate-hs-codes")
    public ResponseEntity<String> populateHsCodes() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("🔧 Populating HS codes...\n\n");
            
            // Get all categories and set proper HS codes
            productCategoryRepository.findAll().forEach(category -> {
                String hsCode;
                switch (category.getCode().toUpperCase()) {
                    case "FOOD":
                        hsCode = "210690"; // Food preparations
                        break;
                    case "STEEL":
                        hsCode = "720851"; // Steel sheets
                        break;
                    case "ELEC":
                        hsCode = "850440"; // Electronic equipment
                        break;
                    case "TOBACCO":
                        hsCode = "240110"; // Tobacco leaves
                        break;
                    case "ALCOHOL":
                        hsCode = "220830"; // Whisky
                        break;
                    default:
                        hsCode = "640411"; // Default
                        break;
                }
                
                category.setHsCode(hsCode);
                productCategoryRepository.save(category);
                result.append(String.format("✅ %s (%s) → HS Code: %s\n", 
                        category.getName(), category.getCode(), hsCode));
            });
            
            result.append("\n🎯 All HS codes populated successfully!");
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to populate HS codes: " + e.getMessage());
        }
    }

    @GetMapping("/check-tariff-data")
    public ResponseEntity<String> checkTariffData() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("📊 Tariff Rate Data Sample:\n\n");
            
            long totalRates = tariffRateRepository.count();
            result.append("Total Tariff Rates: ").append(totalRates).append("\n\n");
            
            if (totalRates == 0) {
                result.append("⚠️ No tariff rates found yet!\n");
                return ResponseEntity.ok(result.toString());
            }
            
            // Get sample data with direct queries to avoid lazy loading issues
            var sampleRates = tariffRateRepository.findAll().stream().limit(10).toList();
            
            // Count zeros vs non-zeros
            long zeroCount = 0;
            long nonZeroCount = 0;
            
            result.append("📋 Sample Tariff Rates:\n");
            for (var rate : sampleRates) {
                boolean isZero = rate.getBaseRate().compareTo(BigDecimal.ZERO) == 0 && 
                               rate.getAdditionalFee().compareTo(BigDecimal.ZERO) == 0;
                
                result.append(String.format("  ID %d: Base=%s%%, Fee=%s %s\n",
                        rate.getId(),
                        rate.getBaseRate(),
                        rate.getAdditionalFee(),
                        isZero ? "❌" : "✅"));
                
                if (isZero) zeroCount++; else nonZeroCount++;
            }
            
            result.append("\n📈 Quick Sample Analysis:\n");
            result.append("  Zero rates in sample: ").append(zeroCount).append("/").append(sampleRates.size()).append("\n");
            result.append("  Non-zero rates in sample: ").append(nonZeroCount).append("/").append(sampleRates.size()).append("\n");
            
            if (zeroCount == sampleRates.size() && sampleRates.size() >= 5) {
                result.append("\n🚨 PROBLEM CONFIRMED: All sample rates are ZERO!\n");
                result.append("This indicates:\n");
                result.append("  1. ❌ API returned zeros from the start (not just quota issue)\n");
                result.append("  2. ❌ Wrong HS codes or country codes\n");
                result.append("  3. ❌ API response format mismatch\n");
                result.append("  4. ❌ MTech API doesn't have data for these combinations\n");
                result.append("\n💡 SOLUTION: Generate realistic mock data instead!\n");
            }
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to check tariff data: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-zero-rates")
    public ResponseEntity<String> deleteZeroRates() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("🗑️ Deleting tariff rates with 0% base rate...\n\n");
            
            long totalBefore = tariffRateRepository.count();
            result.append("Total rates before: ").append(totalBefore).append("\n");
            
            // Find and delete all rates with base_rate = 0
            var zeroRates = tariffRateRepository.findAll().stream()
                .filter(rate -> rate.getBaseRate().compareTo(BigDecimal.ZERO) == 0)
                .toList();
            
            result.append("Found ").append(zeroRates.size()).append(" rates with 0% base rate\n");
            
            tariffRateRepository.deleteAll(zeroRates);
            
            long totalAfter = tariffRateRepository.count();
            result.append("Total rates after: ").append(totalAfter).append("\n\n");
            result.append("✅ Deleted ").append(totalBefore - totalAfter).append(" zero-rate records!\n");
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to delete zero rates: " + e.getMessage());
        }
    }

    @GetMapping("/test-api-call")
    public ResponseEntity<String> testApiCall(
            @RequestParam String origin,
            @RequestParam String dest,
            @RequestParam String hsCode) {
        try {
            // Convert country codes to numeric format
            String originCode = getNumericCountryCode(origin);
            String destCode = getNumericCountryCode(dest);
            
            String testUrl = String.format(
                "https://mtech-api.com/client/api/tariff-data?hscode=%s&country=%s&token=%s",
                hsCode, destCode, "XM7cLeePrzNWsxLuTmBPkT7nskV9bl7p"
            );
            
            StringBuilder result = new StringBuilder();
            result.append("🧪 Testing MTech API Call:\n\n");
            result.append("Origin: ").append(origin).append(" → ").append(originCode).append("\n");
            result.append("Destination: ").append(dest).append(" → ").append(destCode).append("\n");
            result.append("HS Code: ").append(hsCode).append("\n\n");
            result.append("URL: ").append(testUrl).append("\n\n");
            
            RestTemplate restTemplate = new RestTemplate();
            
            // First get raw response to debug
            String rawResponse = null;
            try {
                rawResponse = restTemplate.getForObject(testUrl, String.class);
                result.append("📡 Raw Response Length: ").append(rawResponse != null ? rawResponse.length() : 0).append(" characters\n");
                if (rawResponse != null && rawResponse.length() < 2000) {
                    result.append("📡 Raw Response: ").append(rawResponse).append("\n\n");
                } else if (rawResponse != null) {
                    result.append("📡 Raw Response (first 2000 chars): ").append(rawResponse.substring(0, 2000)).append("...\n\n");
                }
            } catch (Exception e) {
                result.append("📡 Raw Response Error: ").append(e.getMessage()).append("\n\n");
            }
            
            // Try to parse as our expected format FIRST to see if it works
            try {
                MTechDataImporterService.ImportDutyResponse dutyResponse = 
                    restTemplate.getForObject(testUrl, MTechDataImporterService.ImportDutyResponse.class);
                
                result.append("✅ Successfully parsed response!\n\n");
                result.append("🔍 Extracted Data:\n");
                if (dutyResponse != null) {
                    try {
                        result.append("Base Rate: ").append(dutyResponse.getBaseRate()).append("%\n");
                        result.append("Additional Fee: ").append(dutyResponse.getAdditionalFee()).append("\n\n");
                    } catch (Exception parseEx) {
                        result.append("Base Rate: ❌ Parse Error: ").append(parseEx.getMessage()).append("\n");
                        result.append("Stack trace: ").append(parseEx.getClass().getSimpleName()).append("\n\n");
                    }
                    
                    // Show structure info
                    if (dutyResponse.getData() != null) {
                        result.append("📊 Tariff Data Structure:\n");
                        result.append("- Data type: ").append(dutyResponse.getData().getClass().getSimpleName()).append("\n");
                        
                        if (dutyResponse.getData() instanceof java.util.List) {
                            java.util.List<?> list = (java.util.List<?>) dutyResponse.getData();
                            result.append("- Number of results: ").append(list.size()).append("\n\n");
                            
                            // Show first 5 results to find one with actual duty rate
                            int maxToShow = Math.min(5, list.size());
                            for (int i = 0; i < maxToShow; i++) {
                                Object item = list.get(i);
                                if (item instanceof java.util.Map) {
                                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) item;
                                    result.append("Result #").append(i + 1).append(":\n");
                                    result.append("  - HS Code: ").append(map.get("hs_code")).append("\n");
                                    result.append("  - Description: ").append(map.get("desc")).append("\n");
                                    Object tariffRateObj = map.get("tariff_rate");
                                    if (tariffRateObj instanceof java.util.Map) {
                                        java.util.Map<?, ?> rateMap = (java.util.Map<?, ?>) tariffRateObj;
                                        result.append("  - General Rate: ").append(rateMap.get("General Rate of Duty")).append("\n");
                                        result.append("  - Special Rate: ").append(rateMap.get("Special Rate of Duty")).append("\n");
                                        result.append("  - Additional Duties: ").append(rateMap.get("Additional Duties")).append("\n");
                                    }
                                    result.append("\n");
                                }
                            }
                        } else if (dutyResponse.getData() instanceof java.util.Map) {
                            java.util.Map<?, ?> map = (java.util.Map<?, ?>) dutyResponse.getData();
                            result.append("- HS Code: ").append(map.get("hs_code")).append("\n");
                            result.append("- Description: ").append(map.get("desc")).append("\n");
                            Object tariffRateObj = map.get("tariff_rate");
                            if (tariffRateObj instanceof java.util.Map) {
                                java.util.Map<?, ?> rateMap = (java.util.Map<?, ?>) tariffRateObj;
                                result.append("- General Rate: ").append(rateMap.get("General Rate of Duty")).append("\n");
                                result.append("- Special Rate: ").append(rateMap.get("Special Rate of Duty")).append("\n");
                                result.append("- Additional Duties: ").append(rateMap.get("Additional Duties")).append("\n");
                            }
                        }
                    } else {
                        result.append("⚠️ Data is NULL or empty\n");
                    }
                } else {
                    result.append("⚠️ Response object is NULL\n");
                }
            } catch (Exception e) {
                result.append("❌ Parse Error: ").append(e.getMessage()).append("\n");
                result.append("Stack trace: ").append(e.getClass().getName()).append("\n");
            }
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ API test failed: " + e.getMessage());
        }
    }
    
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
            case "JPN":
                return "392"; // Japan
            default:
                throw new IllegalArgumentException("Unknown country code: " + textCode);
        }
    }

    @GetMapping("/diagnose-json")
    public ResponseEntity<String> diagnoseJson() {
        StringBuilder result = new StringBuilder();
        result.append("🔬 JSON Response Structure Diagnosis\n\n");
        
        result.append("Expected JSON structure our code is looking for:\n");
        result.append("{\n");
        result.append("  \"baseRate\": 5.2,\n");
        result.append("  \"additionalFee\": 50.0\n");
        result.append("}\n\n");
        
        result.append("❓ Questions to investigate:\n");
        result.append("1. Does the API return 'baseRate' or 'base_rate' or 'rate'?\n");
        result.append("2. Does the API return 'additionalFee' or 'additional_fee' or 'fee'?\n");
        result.append("3. Are the values nested in a 'data' or 'result' object?\n");
        result.append("4. Are the values strings (\"5.2\") or numbers (5.2)?\n");
        result.append("5. Is the response format different than expected?\n\n");
        
        result.append("🎯 To fix this:\n");
        result.append("We need to see the EXACT raw JSON response from the API.\n");
        result.append("Then we'll update our ImportDutyResponse class to match.\n");
        
        return ResponseEntity.ok(result.toString());
    }
}
