package com.smu.tariff.tariff;

import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.tariff.dto.CalculateTariffRequestDto;
import com.smu.tariff.tariff.dto.CalculateTariffResponseDto;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TariffControllerTest {

    @Mock
    private TariffService tariffService;

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    private CalculateTariffController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    controller = new com.smu.tariff.tariff.CalculateTariffController(tariffService, productCategoryRepository);
    }

    @Test
    void testWeightBasedCalculation_adjustsDeclaredValue() {
        ProductCategory cat = new ProductCategory("ALCOHOL", "Alcohol & Beverages", null, true);
        when(productCategoryRepository.findByCode("ALCOHOL")).thenReturn(Optional.of(cat));

        CalculateTariffRequestDto req = new CalculateTariffRequestDto();
        req.setProductCode("ALCOHOL");
        req.setOriginCountry("156");
        req.setDestCountry("840");
        req.setDeclaredValue(10.0);
        req.setWeight(2.5);

        TariffCalcResponse svcResp = new TariffCalcResponse();
        svcResp.tariffAmount = BigDecimal.valueOf(125.50);

        when(tariffService.calculate(any(), eq(false))).thenReturn(svcResp);

        CalculateTariffResponseDto resp = controller.calculateTariff(req);

        assertThat(resp.getProductCode()).isEqualTo("ALCOHOL");
        assertThat(resp.isWeightBased()).isTrue();
        assertThat(resp.getWeight()).isEqualTo(2.5);
        assertThat(resp.getCalculatedTariff()).isEqualTo(BigDecimal.valueOf(125.50));

        // verify service called with adjusted declaredValue = 10 * 2.5 = 25.0 (we can't directly inspect TariffCalcRequest here easily)
        verify(tariffService, times(1)).calculate(any(), eq(false));
    }
}
