package com.smu.tariff.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.reference.dto.CountryCreateRequest;
import com.smu.tariff.reference.dto.CountryUpdateRequest;
import com.smu.tariff.reference.dto.ProductCategoryCreateRequest;
import com.smu.tariff.reference.dto.ProductCategoryUpdateRequest;
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
    private final ReferenceService referenceService;

    public ReferenceController(CountryRepository countryRepository,
                               ProductCategoryRepository productCategoryRepository,
                               ReferenceService referenceService) {
        this.countryRepository = countryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.referenceService = referenceService;
    }

    @GetMapping("/countries")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<ReferenceOptionDto>> listCountries() {
        // Fetch all countries from the database dynamically
        List<ReferenceOptionDto> response = countryRepository.findAll().stream()
                .map(this::toCountryOption)
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
                .map(this::toCategoryOption)
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
        Country saved = referenceService.createCountry(request);
        return ResponseEntity.ok(toCountryOption(saved));
    }

    @PostMapping("/product-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferenceOptionDto> createProductCategory(@Valid @RequestBody ProductCategoryCreateRequest request) {
        ProductCategory saved = referenceService.createCategory(request);
        return ResponseEntity.ok(toCategoryOption(saved));
    }

    @PutMapping("/countries/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferenceOptionDto> updateCountry(@PathVariable String code,
                                                            @Valid @RequestBody CountryUpdateRequest request) {
        Country updated = referenceService.updateCountry(code, request);
        return ResponseEntity.ok(toCountryOption(updated));
    }

    @DeleteMapping("/countries/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCountry(@PathVariable String code) {
        referenceService.deleteCountry(code);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/product-categories/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferenceOptionDto> updateProductCategory(@PathVariable String code,
                                                                    @Valid @RequestBody ProductCategoryUpdateRequest request) {
        ProductCategory updated = referenceService.updateCategory(code, request);
        return ResponseEntity.ok(toCategoryOption(updated));
    }

    @DeleteMapping("/product-categories/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductCategory(@PathVariable String code) {
        referenceService.deleteCategory(code);
        return ResponseEntity.noContent().build();
    }

    private ReferenceOptionDto toCountryOption(Country country) {
        return new ReferenceOptionDto(country.getCode().toUpperCase(), country.getName());
    }

    private ReferenceOptionDto toCategoryOption(ProductCategory category) {
        return new ReferenceOptionDto(
                category.getCode().toUpperCase(),
                category.getName(),
                category.getHsCode(),
                category.getWeightBased()
        );
    }
}
