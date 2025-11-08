package com.smu.tariff.product;

import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
public class ProductCategorySyncService {
    private static final Logger log = LoggerFactory.getLogger(ProductCategorySyncService.class);

    private final ProductCategoryRepository repository;
    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String mtechBaseUrl;
    private final String mtechToken;

    public ProductCategorySyncService(ProductCategoryRepository repository,
                                      RestTemplateBuilder restTemplateBuilder,
                                      @Value("${simplyduty.api.url:https://api.simplyduty.com/categories}") String apiUrl,
                                      @Value("${mtech.api.base-url:https://mtech-api.com/client/api}") String mtechBaseUrl,
                                      @Value("${mtech.api.token:}") String mtechToken) {
        this.repository = repository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
        this.apiUrl = apiUrl;
        this.mtechBaseUrl = mtechBaseUrl;
        this.mtechToken = mtechToken;
    }

    /**
     * Fetch HS code suggestion from MTech API for a given category/product pair.
     * Returns a ProductCategoryDto with hsCode populated when available.
     */
    public ProductCategoryDto fetchHsCode(String category, String product) {
        if (mtechBaseUrl == null || mtechBaseUrl.isBlank() || mtechToken == null || mtechToken.isBlank()) {
            log.warn("MTech API base URL or token is not configured");
            return null;
        }

        try {
            String url = String.format("%s/hs-code-match?category=%s&product=%s&token=%s",
                    mtechBaseUrl,
                    encodeParam(category),
                    encodeParam(product),
                    encodeParam(mtechToken));

            ResponseEntity<MtechHsMatchResponse> resp = restTemplate.getForEntity(url, MtechHsMatchResponse.class);
            MtechHsMatchResponse body = resp.getBody();
            if (body == null || body.getData() == null || body.getData().getSixDigitCodes() == null || body.getData().getSixDigitCodes().length == 0) {
                log.info("No HS code returned for category='{}' product='{}'", category, product);
                return null;
            }

            MtechHsCodeEntry entry = body.getData().getSixDigitCodes()[0];
            ProductCategoryDto dto = new ProductCategoryDto();
            dto.setCode(category);
            dto.setName(product);
            dto.setHsCode(entry.getSixDigitCode());
            // weightBased unknown here, leave false
            dto.setWeightBased(false);
            return dto;
        } catch (RestClientException ex) {
            log.error("Failed to call MTech hs-code-match API", ex);
            throw ex;
        }
    }

    /**
     * Fetch import duty details from MTech API for given HS code and origin/destination.
     */
    public ImportDutyDto fetchImportDuty(String hscode, String originCountry, String destCountry) {
        if (mtechBaseUrl == null || mtechBaseUrl.isBlank() || mtechToken == null || mtechToken.isBlank()) {
            log.warn("MTech API base URL or token is not configured");
            return null;
        }

        try {
            String url = String.format("%s/import-duty?hscode=%s&origin_country=%s&dest_country=%s&token=%s",
                    mtechBaseUrl,
                    encodeParam(hscode),
                    encodeParam(originCountry),
                    encodeParam(destCountry),
                    encodeParam(mtechToken));

            ResponseEntity<MtechImportDutyResponse> resp = restTemplate.getForEntity(url, MtechImportDutyResponse.class);
            MtechImportDutyResponse body = resp.getBody();
            if (body == null || body.getData() == null) {
                log.info("No import duty data for hscode='{}'", hscode);
                return null;
            }

            MtechImportDutyResponse.Data d = body.getData();
            ImportDutyDto dto = new ImportDutyDto();
            dto.setHscode(hscode);
            dto.setOriginCountry(originCountry);
            dto.setDestCountry(destCountry);
            dto.setBaseRate(d.getBaseRate());
            dto.setAdditionalFee(d.getAdditionalFee());
            dto.setFtaRate(d.getFtaRate());
            dto.setTotalDuty(d.getTotalDuty());
            return dto;
        } catch (RestClientException ex) {
            log.error("Failed to call MTech import-duty API", ex);
            throw ex;
        }
    }

    private String encodeParam(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Fetch categories from SimplyDuty and apply changes to product_category table.
     * This method is transactional - any error will rollback changes.
     */
    @Transactional
    public void fetchAndSyncCategories() {
        log.info("Starting sync of product categories from SimplyDuty: {}", apiUrl);

        ProductCategoryDto[] dtos;
        try {
            ResponseEntity<ProductCategoryDto[]> resp = restTemplate.getForEntity(apiUrl, ProductCategoryDto[].class);
            dtos = resp.getBody();
            if (dtos == null) {
                log.warn("SimplyDuty returned no categories (null body)");
                return;
            }
        } catch (RestClientException ex) {
            log.error("Failed to fetch categories from SimplyDuty", ex);
            throw ex; // Transaction will roll back
        }

        log.info("Fetched {} categories from SimplyDuty", dtos.length);

        Arrays.stream(dtos).forEach(dto -> {
            try {
                processDto(dto);
            } catch (Exception e) {
                log.error("Error processing category {}: {}", dto.getCode(), e.getMessage(), e);
                throw e; // ensure rollback
            }
        });

        log.info("Completed sync of product categories");
    }

    private void processDto(ProductCategoryDto dto) {
        if (dto.getCode() == null || dto.getCode().isBlank()) {
            log.warn("Skipping category with empty code: {}", dto);
            return;
        }

        String code = dto.getCode().trim();

        Optional<ProductCategory> existing = repository.findByCode(code);
        if (existing.isPresent()) {
            ProductCategory pc = existing.get();
            // Update only hs_code and weight_based
            pc.setHsCode(dto.getHsCode());
            pc.setWeightBased(Boolean.valueOf(dto.isWeightBased()));
            repository.save(pc);
            log.debug("Updated category {}: hs_code={}, weight_based={}", code, dto.getHsCode(), dto.isWeightBased());
        } else {
            ProductCategory pc = new ProductCategory(code, dto.getName(), dto.getHsCode(), dto.isWeightBased());
            repository.save(pc);
            log.debug("Inserted new category {}", code);
        }
    }
}
