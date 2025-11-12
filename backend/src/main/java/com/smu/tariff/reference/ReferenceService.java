package com.smu.tariff.reference;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.model.TariffRate;
import com.smu.tariff.reference.dto.CountryCreateRequest;
import com.smu.tariff.reference.dto.CountryUpdateRequest;
import com.smu.tariff.reference.dto.ProductCategoryCreateRequest;
import com.smu.tariff.reference.dto.ProductCategoryUpdateRequest;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.repository.TariffRateRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ReferenceService {

    private final CountryRepository countryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final TariffRateRepository tariffRateRepository;

    public ReferenceService(CountryRepository countryRepository,
                            ProductCategoryRepository productCategoryRepository,
                            TariffRateRepository tariffRateRepository) {
        this.countryRepository = countryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.tariffRateRepository = tariffRateRepository;
    }

    public Country createCountry(CountryCreateRequest request) {
        String code = normalizeCountryCode(request.code);
        String name = normalizeName(request.name);
        countryRepository.findByCode(code).ifPresent(country -> {
            throw new IllegalArgumentException("Country code already exists: " + code);
        });
        Country country = new Country(code, name);
        return countryRepository.save(country);
    }

    public Country updateCountry(String currentCode, CountryUpdateRequest request) {
        Country country = getCountry(currentCode);
        String targetCode = hasText(request.code) ? normalizeCountryCode(request.code) : country.getCode();
        if (!targetCode.equals(country.getCode())) {
            countryRepository.findByCode(targetCode).ifPresent(existing -> {
                throw new IllegalArgumentException("Country code already exists: " + targetCode);
            });
        }
        country.setCode(targetCode);
        country.setName(normalizeName(request.name));
        return countryRepository.save(country);
    }

    public void deleteCountry(String code) {
        Country country = getCountry(code);
        removeTariffsForCountry(country);
        countryRepository.delete(country);
    }

    public ProductCategory createCategory(ProductCategoryCreateRequest request) {
        String code = normalizeCategoryCode(request.code);
        String name = normalizeName(request.name);
        String hsCode = normalizeHsCode(request.hsCode);
        productCategoryRepository.findByCode(code).ifPresent(existing -> {
            throw new IllegalArgumentException("Product category code already exists: " + code);
        });
        ProductCategory category = new ProductCategory(code, name);
        category.setHsCode(hsCode);
        category.setWeightBased(request.weightBased);
        return productCategoryRepository.save(category);
    }

    public ProductCategory updateCategory(String currentCode, ProductCategoryUpdateRequest request) {
        ProductCategory category = getCategory(currentCode);
        String targetCode = hasText(request.code) ? normalizeCategoryCode(request.code) : category.getCode();
        if (!targetCode.equals(category.getCode())) {
            productCategoryRepository.findByCode(targetCode).ifPresent(existing -> {
                throw new IllegalArgumentException("Product category code already exists: " + targetCode);
            });
        }
        category.setCode(targetCode);
        category.setName(normalizeName(request.name));
        category.setHsCode(normalizeHsCode(request.hsCode));
        category.setWeightBased(request.weightBased);
        return productCategoryRepository.save(category);
    }

    public void deleteCategory(String code) {
        ProductCategory category = getCategory(code);
        List<TariffRate> related = tariffRateRepository.findByProductCategory(category);
        if (!related.isEmpty()) {
            tariffRateRepository.deleteAll(related);
        }
        productCategoryRepository.delete(category);
    }

    private void removeTariffsForCountry(Country country) {
        Set<TariffRate> impacted = new HashSet<>();
        impacted.addAll(tariffRateRepository.findByOrigin(country));
        impacted.addAll(tariffRateRepository.findByDestination(country));
        if (!impacted.isEmpty()) {
            tariffRateRepository.deleteAll(impacted);
        }
    }

    private Country getCountry(String code) {
        return countryRepository.findByCode(normalizeCountryCode(code))
                .orElseThrow(() -> new IllegalArgumentException("Unknown country code: " + code));
    }

    private ProductCategory getCategory(String code) {
        return productCategoryRepository.findByCode(normalizeCategoryCode(code))
                .orElseThrow(() -> new IllegalArgumentException("Unknown product category code: " + code));
    }

    private String normalizeCountryCode(String code) {
        if (!hasText(code)) {
            throw new IllegalArgumentException("Country code is required");
        }
        return code.trim().toUpperCase();
    }

    private String normalizeCategoryCode(String code) {
        if (!hasText(code)) {
            throw new IllegalArgumentException("Product category code is required");
        }
        return code.trim().toUpperCase();
    }

    private String normalizeHsCode(String hsCode) {
        if (!hasText(hsCode)) {
            throw new IllegalArgumentException("HS code is required");
        }
        return hsCode.trim().toUpperCase();
    }

    private String normalizeName(String name) {
        if (!hasText(name)) {
            throw new IllegalArgumentException("Name is required");
        }
        return name.trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
