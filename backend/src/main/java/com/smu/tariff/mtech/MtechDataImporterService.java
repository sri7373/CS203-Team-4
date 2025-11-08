package com.smu.tariff.mtech;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.repository.TariffRateRepository;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class MtechDataImporterService {
    private final Logger logger = LoggerFactory.getLogger(MtechDataImporterService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CountryRepository countryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final TariffRateRepository tariffRateRepository;

    public MtechDataImporterService(RestTemplate restTemplate,
                                    ObjectMapper objectMapper,
                                    CountryRepository countryRepository,
                                    ProductCategoryRepository productCategoryRepository,
                                    TariffRateRepository tariffRateRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.countryRepository = countryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.tariffRateRepository = tariffRateRepository;
    }

    @Transactional
    public ImportResultDto importFromUrl(String url) {
        int imported = 0;
        int skipped = 0;
        try {
            logger.info("Importing tariff data from URL: {}", url);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
            String body = resp.getBody();
            if (body == null || body.isBlank()) {
                return new ImportResultDto(0,0,"Empty response");
            }

            // Try JSON first
            if (looksLikeJson(body)) {
                JsonNode root = objectMapper.readTree(body);
                List<Map<String, Object>> rows = new ArrayList<>();
                if (root.isArray()) {
                    rows = objectMapper.convertValue(root, new TypeReference<List<Map<String, Object>>>(){});
                } else if (root.has("data") && root.get("data").isArray()) {
                    rows = objectMapper.convertValue(root.get("data"), new TypeReference<List<Map<String, Object>>>(){});
                } else if (root.has("rows") && root.get("rows").isArray()) {
                    rows = objectMapper.convertValue(root.get("rows"), new TypeReference<List<Map<String, Object>>>(){});
                } else {
                    // If single object, wrap
                    Map<String,Object> map = objectMapper.convertValue(root, new TypeReference<Map<String,Object>>(){});
                    rows.add(map);
                }

                for (Map<String,Object> r : rows) {
                    boolean ok = processRowMap(r);
                    if (ok) imported++; else skipped++;
                }

            } else {
                // treat as HTML and try to extract table rows
                Document doc = Jsoup.parse(body);
                // find table-like rows
                doc.select("tr").forEach(tr -> {
                    // map columns by position - best-effort: origin,destination,product,baseRate,additionalFee,effectiveFrom,effectiveTo
                });
                // HTML parsing is best-effort; return skipped
                return new ImportResultDto(0,0,"HTML import not implemented in detail; please provide a JSON endpoint or structured data");
            }
        } catch (Exception ex) {
            logger.error("Import failed", ex);
            return new ImportResultDto(imported, skipped, "Error: " + ex.getMessage());
        }

        return new ImportResultDto(imported, skipped, "OK");
    }

    private boolean processRowMap(Map<String,Object> r) {
        try {
            String originRaw = str(r.getOrDefault("origin", r.get("origin_country")));
            String destRaw = str(r.getOrDefault("destination", r.get("destination_country")));
            String productRaw = str(r.getOrDefault("product", r.getOrDefault("product_name", r.get("commodity"))));
            String baseRateRaw = str(r.getOrDefault("baseRate", r.getOrDefault("rate", r.get("tariff"))));
            String feeRaw = str(r.getOrDefault("additionalFee", r.getOrDefault("fee", "0")));
            String effFromRaw = str(r.getOrDefault("effectiveFrom", r.getOrDefault("start_date", r.get("from"))));
            String effToRaw = str(r.getOrDefault("effectiveTo", r.getOrDefault("end_date", r.get("to"))));

            Country origin = resolveCountry(originRaw);
            Country dest = resolveCountry(destRaw);
            if (origin == null || dest == null) {
                logger.warn("Skipping row because origin or destination could not be resolved: origin='{}' dest='{}'", originRaw, destRaw);
                return false;
            }

            ProductCategory pc = resolveProductCategory(productRaw);
            if (pc == null) {
                logger.warn("Skipping row because product category could not be resolved: {}", productRaw);
                return false;
            }

            BigDecimal baseRate = parseRate(baseRateRaw);
            BigDecimal fee = parseFee(feeRaw);
            LocalDate effFrom = parseDateOrNow(effFromRaw);
            LocalDate effTo = parseDateOrNull(effToRaw);

            // If product looks like alcohol or tobacco, ensure weightBased
            if (isAlcoholOrTobacco(pc)) {
                if (!pc.getWeightBased()) {
                    pc.setWeightBased(Boolean.TRUE);
                    productCategoryRepository.save(pc);
                }
            }

            // Check for existing top rate and update if same effectiveFrom
            Optional<TariffRate> existing = tariffRateRepository.findTop1ByOriginAndDestinationAndProductCategoryOrderByEffectiveFromDesc(origin, dest, pc);
            if (existing.isPresent() && Objects.equals(existing.get().getEffectiveFrom(), effFrom)) {
                TariffRate t = existing.get();
                t.setBaseRate(baseRate);
                t.setAdditionalFee(fee);
                t.setEffectiveTo(effTo);
                tariffRateRepository.save(t);
            } else {
                TariffRate tr = new TariffRate(origin, dest, pc, baseRate, fee, effFrom, effTo);
                tariffRateRepository.save(tr);
            }

            return true;
        } catch (Exception ex) {
            logger.warn("Failed to process row: {}", ex.getMessage());
            return false;
        }
    }

    private boolean looksLikeJson(String s) {
        String t = s.trim();
        return t.startsWith("{") || t.startsWith("[");
    }

    private String str(Object o) { return o == null ? null : o.toString().trim(); }

    private Country resolveCountry(String raw) {
        if (raw == null) return null;
        String r = raw.trim();
        if (r.length() == 3) {
            Optional<Country> byCode = countryRepository.findByCode(r.toUpperCase());
            if (byCode.isPresent()) return byCode.get();
        }
        Optional<Country> byName = countryRepository.findByNameIgnoreCase(r);
        return byName.orElse(null);
    }

    private ProductCategory resolveProductCategory(String raw) {
        if (raw == null) return null;
        String r = raw.trim();
        // try code first
        Optional<ProductCategory> byCode = productCategoryRepository.findByCode(r.toUpperCase());
        if (byCode.isPresent()) return byCode.get();
        // try name exact ignore case
        Optional<ProductCategory> byName = productCategoryRepository.findByNameIgnoreCase(r);
        if (byName.isPresent()) return byName.get();

        // fallback: partial match against all categories
        String norm = normalize(r);
        for (ProductCategory pc : productCategoryRepository.findAll()) {
            if (normalize(pc.getName()).contains(norm) || norm.contains(normalize(pc.getName()))) {
                return pc;
            }
        }
        return null;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private boolean isAlcoholOrTobacco(ProductCategory pc) {
        if (pc == null) return false;
        String code = pc.getCode() == null ? "" : pc.getCode().toLowerCase();
        String name = pc.getName() == null ? "" : pc.getName().toLowerCase();
        return code.contains("alcohol") || code.contains("tobacco") || name.contains("alcohol") || name.contains("tobacco");
    }

    private BigDecimal parseRate(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        String s = raw.replaceAll("%", "").trim();
        try {
            BigDecimal v = new BigDecimal(s);
            // if value looks like percent >1, convert to decimal
            if (v.compareTo(BigDecimal.ONE) > 0) {
                return v.divide(new BigDecimal(100));
            }
            return v;
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseFee(String raw) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        String s = raw.replaceAll("[^0-9.\\-]", "");
        try {
            return new BigDecimal(s);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDateOrNow(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(raw); } catch (Exception ex) { return LocalDate.now(); }
    }

    private LocalDate parseDateOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return LocalDate.parse(raw); } catch (Exception ex) { return null; }
    }
}
