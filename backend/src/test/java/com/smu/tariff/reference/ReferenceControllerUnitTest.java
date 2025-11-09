package com.smu.tariff.reference;

import com.smu.tariff.country.Country;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.reference.ReferenceController;
import com.smu.tariff.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceControllerUnitTest {

    private com.smu.tariff.country.CountryRepository countryRepository;
    private com.smu.tariff.repository.ProductCategoryRepository productCategoryRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        countryRepository = mock(com.smu.tariff.country.CountryRepository.class);
        productCategoryRepository = mock(com.smu.tariff.repository.ProductCategoryRepository.class);
        ReferenceController controller = new ReferenceController(countryRepository, productCategoryRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listCountries_returnsFallbackAndResolvedNames() throws Exception {
        when(countryRepository.findAll()).thenReturn(List.of(new Country("SGP", "Singapore")));

        mockMvc.perform(get("/api/reference/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SGP"))
                .andExpect(jsonPath("$[0].label").value("Singapore"));
    }

    @Test
    void listProductCategories_returnsFallbackAndResolvedNames() throws Exception {
        when(productCategoryRepository.findAll()).thenReturn(List.of(new ProductCategory() {{ setCode("ELEC"); setName("Electronics"); }}));

    mockMvc.perform(get("/api/reference/product-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("STEEL"))
        .andExpect(jsonPath("$[0].label").value("Steel Products"));
    }
}
