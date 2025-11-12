package com.smu.tariff.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smu.tariff.country.Country;
import com.smu.tariff.country.CountryRepository;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.reference.dto.CountryCreateRequest;
import com.smu.tariff.reference.dto.ProductCategoryCreateRequest;
import com.smu.tariff.repository.ProductCategoryRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ReferenceControllerTest {

    @Mock
    CountryRepository countryRepository;

    @Mock
    ProductCategoryRepository productCategoryRepository;

    @Mock
    ReferenceService referenceService;

    @InjectMocks
    ReferenceController referenceController;

    private Country singapore;
    private Country unitedStates;
    private ProductCategory electronics;
    private ProductCategory steel;

    @BeforeEach
    void init() {
        singapore = new Country("SGP", "Singapore");
        unitedStates = new Country("USA", "United States");
        electronics = new ProductCategory("ELEC", "Electronics");
        steel = new ProductCategory("STEEL", "Steel Products");
    }

    @Test
    void listCountriesReturnsSortedResponse() {
        when(countryRepository.findAll()).thenReturn(List.of(singapore, unitedStates));

        ResponseEntity<List<ReferenceOptionDto>> response = referenceController.listCountries();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(o -> o.code)
                .containsExactly("SGP", "USA");
        assertThat(response.getBody()).extracting(o -> o.name)
                .containsExactly("Singapore", "United States");
    }

    @Test
    void listCountriesFallsBackWhenEmpty() {
        when(countryRepository.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<List<ReferenceOptionDto>> response = referenceController.listCountries();

        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get(0).code).isEqualTo("SGP");
    }

    @Test
    void listProductCategoriesUsesRepositoryOrder() {
        when(productCategoryRepository.findAll()).thenReturn(List.of(electronics, steel));

        ResponseEntity<List<ReferenceOptionDto>> response = referenceController.listProductCategories();

        assertThat(response.getBody()).extracting(o -> o.code)
                .containsExactly("ELEC", "STEEL");
    }

    @Test
    void createCountryDelegatesToService() {
        CountryCreateRequest request = new CountryCreateRequest();
        request.code = "mys";
        request.name = "Malaysia";

        when(referenceService.createCountry(request)).thenReturn(new Country("MYS", "Malaysia"));

        ResponseEntity<ReferenceOptionDto> response = referenceController.createCountry(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().code).isEqualTo("MYS");
        verify(referenceService).createCountry(request);
    }

    @Test
    void createProductCategoryDelegatesToService() {
        ProductCategoryCreateRequest request = new ProductCategoryCreateRequest();
        request.code = "chem";
        request.name = "Chemicals";
        request.hsCode = "1234";
        request.weightBased = true;

        when(referenceService.createCategory(request))
                .thenReturn(new ProductCategory("CHEM", "Chemicals"));

        ResponseEntity<ReferenceOptionDto> response = referenceController.createProductCategory(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().code).isEqualTo("CHEM");
        verify(referenceService).createCategory(request);
    }

    @Test
    void deleteCountryInvokesService() {
        ResponseEntity<Void> response = referenceController.deleteCountry("SGP");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(referenceService).deleteCountry("SGP");
    }

    @Test
    void deleteProductCategoryInvokesService() {
        ResponseEntity<Void> response = referenceController.deleteProductCategory("ELEC");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(referenceService).deleteCategory("ELEC");
    }
}
