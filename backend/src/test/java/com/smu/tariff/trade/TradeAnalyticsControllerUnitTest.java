package com.smu.tariff.trade;

import com.smu.tariff.exception.GlobalExceptionHandler;
import com.smu.tariff.exception.InvalidTariffRequestException;
import com.smu.tariff.trade.dto.CountryTradeInsightsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradeAnalyticsControllerUnitTest {

    private TradeAnalyticsService tradeAnalyticsService;
    private MockMvc mockMvc;
    // no ObjectMapper needed for these simple assertions

    @BeforeEach
    void setUp() {
        tradeAnalyticsService = mock(TradeAnalyticsService.class);
        TradeAnalyticsController controller = new TradeAnalyticsController(tradeAnalyticsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getInsights_returns200_whenServiceProvidesData() throws Exception {
        CountryTradeInsightsDto dto = new CountryTradeInsightsDto();
        dto.countryCode = "SGP";
        dto.countryName = "Singapore";

        when(tradeAnalyticsService.getCountryInsights("SGP")).thenReturn(dto);

        mockMvc.perform(get("/api/trade/insights").param("country", "SGP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("SGP"));
    }

    @Test
    void getInsights_returns400_whenServiceThrowsInvalid() throws Exception {
        when(tradeAnalyticsService.getCountryInsights("XXX")).thenThrow(new InvalidTariffRequestException("Unknown country code"));

        mockMvc.perform(get("/api/trade/insights").param("country", "XXX"))
                .andExpect(status().isBadRequest());
    }
}
