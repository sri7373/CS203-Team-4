package com.smu.tariff.reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.product.ProductCategoryRepository;

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
        Map<String, String> resolvedNames = countryRepository.findAll().stream()
                .collect(Collectors.toMap(country -> country.getCode().toUpperCase(), country -> country.getName(),
                        (a, b) -> a));

        List<ReferenceOptionDto> response = new ArrayList<>();
        for (String code : COUNTRY_CODES) {
            String name = resolvedNames.getOrDefault(code, COUNTRY_FALLBACK_NAMES.getOrDefault(code, code));
            response.add(new ReferenceOptionDto(code, name));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product-categories")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<ReferenceOptionDto>> listProductCategories() {
        Map<String, String> resolvedNames = productCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(cat -> cat.getCode().toUpperCase(), cat -> cat.getName(),
                        (a, b) -> a));

        List<ReferenceOptionDto> response = new ArrayList<>();
        for (String code : PRODUCT_CATEGORY_CODES) {
            String name = resolvedNames.getOrDefault(code,
                    PRODUCT_CATEGORY_FALLBACK_NAMES.getOrDefault(code, code));
            response.add(new ReferenceOptionDto(code, name));
        }

        return ResponseEntity.ok(response);
    }
}
