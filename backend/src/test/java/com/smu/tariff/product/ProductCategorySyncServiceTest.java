package com.smu.tariff.product;

import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductCategorySyncServiceTest {

    @Mock
    private ProductCategoryRepository repository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private ProductCategorySyncService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        service = new ProductCategorySyncService(repository, restTemplateBuilder, "http://example/api/categories");
    }

    @Test
    void testFetchAndSyncCategories_updatesExistingAndInsertsNew() {
        // existing category
        ProductCategory existing = new ProductCategory("ALCOHOL","Alcohol & Beverages");
        existing.setHsCode(null);
        existing.setWeightBased(false);

        when(repository.findByCode("ALCOHOL")).thenReturn(Optional.of(existing));

        // new category
        when(repository.findByCode("NEWCAT")).thenReturn(Optional.empty());

        ProductCategoryDto dto1 = new ProductCategoryDto();
        dto1.setCode("ALCOHOL");
        dto1.setName("Alcohol & Beverages");
        dto1.setHsCode("2203");
        dto1.setWeightBased(true);

        ProductCategoryDto dto2 = new ProductCategoryDto();
        dto2.setCode("NEWCAT");
        dto2.setName("New Category");
        dto2.setHsCode("1234");
        dto2.setWeightBased(false);

        ProductCategoryDto[] arr = new ProductCategoryDto[]{dto1, dto2};

        when(restTemplate.getForEntity(any(String.class), eq(ProductCategoryDto[].class)))
                .thenReturn(ResponseEntity.ok(arr));

        service.fetchAndSyncCategories();

        // existing should be updated (hsCode and weightBased changed)
        ArgumentCaptor<ProductCategory> captor = ArgumentCaptor.forClass(ProductCategory.class);
        verify(repository, times(2)).save(captor.capture()); // one for update, one for insert

        ProductCategory saved1 = captor.getAllValues().get(0);
        assertThat(saved1.getCode()).isEqualTo("ALCOHOL");
        assertThat(saved1.getHsCode()).isEqualTo("2203");
        assertThat(saved1.getWeightBased()).isTrue();

        ProductCategory saved2 = captor.getAllValues().get(1);
        assertThat(saved2.getCode()).isEqualTo("NEWCAT");
        assertThat(saved2.getName()).isEqualTo("New Category");
        assertThat(saved2.getHsCode()).isEqualTo("1234");
        assertThat(saved2.getWeightBased()).isFalse();
    }
}
