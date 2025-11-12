package com.smu.tariff.trade;

import com.smu.tariff.trade.dto.CountryTradeInsightsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trade")
public class TradeAnalyticsController {

    private final TradeAnalyticsService tradeAnalyticsService;

    public TradeAnalyticsController(TradeAnalyticsService tradeAnalyticsService) {
        this.tradeAnalyticsService = tradeAnalyticsService;
    }

    @GetMapping("/insights")
    public ResponseEntity<CountryTradeInsightsDto> getInsights(@RequestParam(value = "country", required = false) String countryCode) {
        // Make parameter optional at the framework level and validate explicitly so
        // our application exception handling returns a friendly 400 response.
        if (countryCode == null || countryCode.trim().isEmpty()) {
            throw new com.smu.tariff.exception.InvalidTariffRequestException("Country code is required");
        }

        return ResponseEntity.ok(tradeAnalyticsService.getCountryInsights(countryCode));
    }
}
