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
    public ResponseEntity<CountryTradeInsightsDto> getInsights(@RequestParam("country") String countryCode) {
        return ResponseEntity.ok(tradeAnalyticsService.getCountryInsights(countryCode));
    }
}
