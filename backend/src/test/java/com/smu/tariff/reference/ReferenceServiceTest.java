package com.smu.tariff.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ReferenceServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private TariffRateRepository tariffRateRepository;

    @InjectMocks
    private ReferenceService service;

    private Country existingCountry;
    private ProductCategory existingCategory;

    @BeforeEach
    void init() {
        existingCountry = new Country("SGP", "Singapore");
        existingCategory = new ProductCategory("ELEC", "Electronics");
    }

    @Test
    void createCountryNormalizesInputsAndPersists() {
        CountryCreateRequest request = new CountryCreateRequest();
        request.code = " sgp ";
        request.name = "  Singapore  ";

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.empty());
        ArgumentCaptor<Country> captor = ArgumentCaptor.forClass(Country.class);
        when(countryRepository.save(any(Country.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Country saved = service.createCountry(request);

        verify(countryRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SGP");
        assertThat(captor.getValue().getName()).isEqualTo("Singapore");
        assertThat(saved.getCode()).isEqualTo("SGP");
    }

    @Test
    void createCountryRejectsDuplicates() {
        CountryCreateRequest request = new CountryCreateRequest();
        request.code = "SGP";
        request.name = "Singapore";

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(existingCountry));

        assertThatThrownBy(() -> service.createCountry(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        verify(countryRepository, never()).save(any());
    }

    @Test
    void updateCountryChangesCodeWhenUnique() {
        CountryUpdateRequest request = new CountryUpdateRequest();
        request.code = "mys";
        request.name = "  Malaysia  ";

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(existingCountry));
        when(countryRepository.findByCode("MYS")).thenReturn(Optional.empty());
        when(countryRepository.save(any(Country.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Country updated = service.updateCountry("SGP", request);

        assertThat(updated.getCode()).isEqualTo("MYS");
        assertThat(updated.getName()).isEqualTo("Malaysia");
    }

    @Test
    void deleteCountryRemovesRelatedTariffs() {
        TariffRate outbound = sampleTariff(existingCountry, new Country("USA", "United States"));
        TariffRate inbound = sampleTariff(new Country("CHN", "China"), existingCountry);

        when(countryRepository.findByCode("SGP")).thenReturn(Optional.of(existingCountry));
        when(tariffRateRepository.findByOrigin(existingCountry)).thenReturn(List.of(outbound));
        when(tariffRateRepository.findByDestination(existingCountry)).thenReturn(List.of(inbound));

        ArgumentCaptor<Iterable<TariffRate>> captor = ArgumentCaptor.forClass(Iterable.class);

        service.deleteCountry("SGP");

        verify(tariffRateRepository).deleteAll(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(outbound, inbound);
        verify(countryRepository).delete(existingCountry);
    }

    @Test
    void createCategoryNormalizesFields() {
        ProductCategoryCreateRequest request = new ProductCategoryCreateRequest();
        request.code = " elec ";
        request.name = " Electronics ";
        request.hsCode = " 8517 ";
        request.weightBased = true;

        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.empty());
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductCategory saved = service.createCategory(request);

        assertThat(saved.getCode()).isEqualTo("ELEC");
        assertThat(saved.getName()).isEqualTo("Electronics");
        assertThat(saved.getHsCode()).isEqualTo("8517");
        assertThat(saved.getWeightBased()).isTrue();
    }

    @Test
    void updateCategoryValidatesDuplicateCodes() {
        ProductCategoryUpdateRequest request = new ProductCategoryUpdateRequest();
        request.name = "Consumer Electronics";
        request.hsCode = " 8517 ";
        request.weightBased = Boolean.FALSE;
        request.code = " gadgets ";

        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(existingCategory));
        when(productCategoryRepository.findByCode("GADGETS")).thenReturn(Optional.empty());
        when(productCategoryRepository.save(any(ProductCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductCategory updated = service.updateCategory("ELEC", request);

        assertThat(updated.getCode()).isEqualTo("GADGETS");
        assertThat(updated.getName()).isEqualTo("Consumer Electronics");
        assertThat(updated.getHsCode()).isEqualTo("8517");
        assertThat(updated.getWeightBased()).isFalse();
    }

    @Test
    void deleteCategoryRemovesRelatedTariffs() {
        ProductCategory category = existingCategory;
        TariffRate rate = sampleTariff(new Country("USA", "United States"), new Country("CHN", "China"));
        rate.setProductCategory(category);

        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(category));
        when(tariffRateRepository.findByProductCategory(category)).thenReturn(List.of(rate));

        service.deleteCategory("ELEC");

        verify(tariffRateRepository).deleteAll(List.of(rate));
        verify(productCategoryRepository).delete(category);
    }

    private TariffRate sampleTariff(Country origin, Country destination) {
        TariffRate rate = new TariffRate();
        rate.setOrigin(origin);
        rate.setDestination(destination);
        rate.setProductCategory(existingCategory);
        rate.setBaseRate(new BigDecimal("5.0000"));
        rate.setAdditionalFee(new BigDecimal("10.00"));
        rate.setEffectiveFrom(LocalDate.now());
        return rate;
    }
}
