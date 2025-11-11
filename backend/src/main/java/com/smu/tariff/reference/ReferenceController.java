package com.smu.tariff.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.reference.dto.CountryCreateRequest;
import com.smu.tariff.reference.dto.ProductCategoryCreateRequest;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reference")
public class ReferenceController {

    private static final List<String> COUNTRY_CODES = List.of("SGP", "USA", "CHN", "MYS", "IDN");
    private static final Map<String, String> COUNTRY_FALLBACK_NAMES = Map.of(
            "SGP", "Singapore",
            "USA", "United States",
            "CHN", "China",
            "MYS", "Malaysia",
            "IDN", "Indonesia"
    );

    private static final List<String> PRODUCT_CATEGORY_CODES = List.of("STEEL", "ELEC", "FOOD");
    private static final Map<String, String> PRODUCT_CATEGORY_FALLBACK_NAMES = Map.of(
            "STEEL", "Steel Products",
            "ELEC", "Electronics",
            "FOOD", "Food Commodities"
    );

    private final CountryRepository countryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public ReferenceController(CountryRepository countryRepository,
                               ProductCategoryRepository productCategoryRepository) {
        this.countryRepository = countryRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @GetMapping("/countries")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<ReferenceOptionDto>> listCountries() {
        // Fetch all countries from the database dynamically
        List<ReferenceOptionDto> response = countryRepository.findAll().stream()
                .map(country -> new ReferenceOptionDto(
                        country.getCode().toUpperCase(), 
                        country.getName()))
                .sorted((a, b) -> a.name.compareTo(b.name)) // Sort by name
                .collect(Collectors.toList());

        // Fallback to hardcoded list if database is empty
        if (response.isEmpty()) {
            response = new ArrayList<>();
            for (String code : COUNTRY_CODES) {
                String name = COUNTRY_FALLBACK_NAMES.getOrDefault(code, code);
                response.add(new ReferenceOptionDto(code, name));
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product-categories")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<ReferenceOptionDto>> listProductCategories() {
        // Fetch all product categories from the database dynamically
        List<ReferenceOptionDto> response = productCategoryRepository.findAll().stream()
                .map(cat -> new ReferenceOptionDto(
                        cat.getCode().toUpperCase(),
                        cat.getName(),
                        cat.getHsCode(),
                        cat.getWeightBased()))
                .sorted((a, b) -> a.name.compareTo(b.name)) // Sort by name
                .collect(Collectors.toList());

        // Fallback to hardcoded list if database is empty
        if (response.isEmpty()) {
            response = new ArrayList<>();
            for (String code : PRODUCT_CATEGORY_CODES) {
                String name = PRODUCT_CATEGORY_FALLBACK_NAMES.getOrDefault(code, code);
                response.add(new ReferenceOptionDto(code, name));
            }
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/countries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferenceOptionDto> createCountry(@Valid @RequestBody CountryCreateRequest request) {
        String code = request.code.trim().toUpperCase();
        String name = request.name.trim();
        if (countryRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Country code already exists: " + code);
        }
        Country country = new Country(code, name);
        Country saved = countryRepository.save(country);
        return ResponseEntity.ok(new ReferenceOptionDto(saved.getCode(), saved.getName()));
    }

    @PostMapping("/product-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferenceOptionDto> createProductCategory(@Valid @RequestBody ProductCategoryCreateRequest request) {
        String code = request.code.trim().toUpperCase();
        if (productCategoryRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Product category code already exists: " + code);
        }
        ProductCategory category = new ProductCategory();
        category.setCode(code);
        category.setName(request.name.trim());
        category.setHsCode(request.hsCode.trim());
        category.setWeightBased(request.weightBased);

        ProductCategory saved = productCategoryRepository.save(category);
        return ResponseEntity.ok(new ReferenceOptionDto(
                saved.getCode(),
                saved.getName(),
                saved.getHsCode(),
                saved.getWeightBased()
        ));
    }
}
