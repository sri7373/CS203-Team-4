package com.smu.tariff.tariff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smu.tariff.model.ProductCategory;
import com.smu.tariff.repository.ProductCategoryRepository;
import com.smu.tariff.exception.GlobalExceptionHandler;
import com.smu.tariff.tariff.dto.CalculateTariffRequestDto;
import com.smu.tariff.tariff.dto.TariffCalcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CalculateTariffControllerUnitTest {

    private MockMvc mockMvc;
    private TariffService tariffService;
    private ProductCategoryRepository productCategoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tariffService = mock(TariffService.class);
        productCategoryRepository = mock(ProductCategoryRepository.class);
    CalculateTariffController controller = new CalculateTariffController(tariffService, productCategoryRepository);
    // Register the global exception handler so thrown InvalidTariffRequestException is mapped to a 400 Bad Request
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    }

    @Test
    void calculateTariff_weightBased_returns200_andCalculatedTariff() throws Exception {
        ProductCategory cat = new ProductCategory();
        cat.setCode("ELEC");
        cat.setHsCode("8542");
        cat.setWeightBased(true);

        when(productCategoryRepository.findByCode("ELEC")).thenReturn(Optional.of(cat));

        TariffCalcResponse svcResp = new TariffCalcResponse();
        svcResp.tariffAmount = BigDecimal.valueOf(123.45);

        when(tariffService.calculate(any(), eq(false))).thenReturn(svcResp);

        CalculateTariffRequestDto req = new CalculateTariffRequestDto();
        req.setProductCode("ELEC");
        req.setOriginCountry("SGP");
        req.setDestCountry("USA");
        req.setDeclaredValue(10.0);
        req.setWeight(2.0);

        mockMvc.perform(post("/api/calculate-tariff")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("ELEC"))
                .andExpect(jsonPath("$.hsCode").value("8542"))
                .andExpect(jsonPath("$.weightBased").value(true))
                .andExpect(jsonPath("$.weight").value(2.0))
                .andExpect(jsonPath("$.calculatedTariff").value(123.45));
    }

    @Test
    void calculateTariff_unknownProduct_returnsServerError() throws Exception {
        when(productCategoryRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        CalculateTariffRequestDto req = new CalculateTariffRequestDto();
        req.setProductCode("INVALID");
        req.setOriginCountry("SGP");
        req.setDestCountry("USA");
        req.setDeclaredValue(100.0);

    mockMvc.perform(post("/api/calculate-tariff")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(req)))
        // GlobalExceptionHandler maps InvalidTariffRequestException -> 400 Bad Request
        .andExpect(status().isBadRequest());
    }
}
