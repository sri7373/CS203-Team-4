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

    public ProductCategorySyncService(ProductCategoryRepository repository,
                                      RestTemplateBuilder restTemplateBuilder,
                                      @Value("${simplyduty.api.url:https://api.simplyduty.com/categories}") String apiUrl) {
        this.repository = repository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
        this.apiUrl = apiUrl;
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
            pc.setWeightBased(dto.isWeightBased());
            repository.save(pc);
            log.debug("Updated category {}: hs_code={}, weight_based={}", code, dto.getHsCode(), dto.isWeightBased());
        } else {
            ProductCategory pc = new ProductCategory(code, dto.getName(), dto.getHsCode(), dto.isWeightBased());
            repository.save(pc);
            log.debug("Inserted new category {}", code);
        }
    }
}
