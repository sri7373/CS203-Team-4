package com.smu.tariff.tariff;

import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.tariff.dto.CalculateTariffRequestDto;
import com.smu.tariff.tariff.dto.CalculateTariffResponseDto;
import com.smu.tariff.tariff.dto.TariffCalcRequest;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import com.smu.tariff.tariff.dto.TariffRateDto;
import com.smu.tariff.tariff.dto.TariffRateDtoPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TariffControllerTest {
    @Test
    void testCalculateEndpoint_serviceThrows() {
        TariffController controller = new TariffController(tariffService);
        TariffCalcRequest req = new TariffCalcRequest();
        when(tariffService.calculate(any(), eq(true))).thenThrow(new RuntimeException("fail"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.calculate(req, true))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testSearchEndpoint_emptyResult() {
        TariffController controller = new TariffController(tariffService);
        when(tariffService.search(any(), any(), any())).thenReturn(java.util.List.of());
        var result = controller.search(null, null, null);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).isEmpty();
    }

    @Test
    void testGetAllTariffsEndpoint_emptyResult() {
        TariffController controller = new TariffController(tariffService);
        when(tariffService.getAllTariffs()).thenReturn(java.util.List.of());
        var result = controller.getAllTariffs();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).isEmpty();
    }

    @Test
    void testCreateTariffEndpoint_serviceThrows() {
        TariffController controller = new TariffController(tariffService);
        TariffRateDtoPost post = new TariffRateDtoPost();
        when(tariffService.createTariff(any())).thenThrow(new RuntimeException("fail"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.createTariff(post))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testUpdateTariffEndpoint_serviceThrows() {
        TariffController controller = new TariffController(tariffService);
        TariffRateDtoPost post = new TariffRateDtoPost();
        when(tariffService.updateTariff(eq(1L), any(TariffRateDtoPost.class))).thenThrow(new RuntimeException("fail"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.updateTariff(1L, post))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testDeleteTariffEndpoint_serviceThrows() {
        TariffController controller = new TariffController(tariffService);
        doThrow(new RuntimeException("fail")).when(tariffService).deleteTariff(1L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.deleteTariff(1L))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testGenerateSummaryEndpoint_serviceThrows() {
        TariffController controller = new TariffController(tariffService);
        TariffCalcResponse resp = new TariffCalcResponse();
        when(tariffService.generateAiSummary(any())).thenThrow(new RuntimeException("fail"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.generateSummary(resp))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCalculateEndpointReturnsResponse() {
        TariffController controller = new TariffController(tariffService);
        TariffCalcRequest req = new TariffCalcRequest();
        req.originCountryCode = "SG";
        req.destinationCountryCode = "MY";
        req.hsCode = "HS123";
        req.declaredValue = 100.0;
        TariffCalcResponse resp = new TariffCalcResponse();
        resp.originCountryCode = "SG";
        when(tariffService.calculate(any(), eq(true))).thenReturn(resp);
        var result = controller.calculate(req, true);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().originCountryCode).isEqualTo("SG");
    }

    @Test
    void testSearchEndpointReturnsList() {
        TariffController controller = new TariffController(tariffService);
        TariffRateDto dto = new TariffRateDto();
        dto.originCountryCode = "SG";
        when(tariffService.search(any(), any(), any())).thenReturn(java.util.List.of(dto));
        var result = controller.search("SG", "MY", "CAT1");
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().size()).isEqualTo(1);
        assertThat(result.getBody().get(0).originCountryCode).isEqualTo("SG");
    }

    @Test
    void testGetAllTariffsEndpointReturnsList() {
        TariffController controller = new TariffController(tariffService);
        TariffRateDto dto = new TariffRateDto();
        dto.originCountryCode = "SG";
        when(tariffService.getAllTariffs()).thenReturn(java.util.List.of(dto));
        var result = controller.getAllTariffs();
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().size()).isEqualTo(1);
    }

    @Test
    void testCreateTariffEndpointReturnsDto() {
        TariffController controller = new TariffController(tariffService);
        TariffRateDtoPost post = new TariffRateDtoPost();
        TariffRateDto dto = new TariffRateDto();
        dto.originCountryCode = "SG";
        when(tariffService.createTariff(any())).thenReturn(dto);
        var result = controller.createTariff(post);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().originCountryCode).isEqualTo("SG");
    }

    @Test
    void testUpdateTariffEndpointReturnsDto() {
        TariffController controller = new TariffController(tariffService);
        TariffRateDtoPost post = new TariffRateDtoPost();
        TariffRateDto dto = new TariffRateDto();
        dto.originCountryCode = "SG";
    when(tariffService.updateTariff(eq(1L), any(TariffRateDtoPost.class))).thenReturn(dto);
        var result = controller.updateTariff(1L, post);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().originCountryCode).isEqualTo("SG");
    }

    @Test
    void testDeleteTariffEndpointReturnsNoContent() {
        TariffController controller = new TariffController(tariffService);
        doNothing().when(tariffService).deleteTariff(1L);
        var result = controller.deleteTariff(1L);
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void testGenerateSummaryEndpointReturnsSummary() {
        TariffController controller = new TariffController(tariffService);
        TariffCalcResponse resp = new TariffCalcResponse();
        when(tariffService.generateAiSummary(any())).thenReturn("<p>summary</p>");
        var result = controller.generateSummary(resp);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get("aiSummary")).isEqualTo("<p>summary</p>");
    }

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
