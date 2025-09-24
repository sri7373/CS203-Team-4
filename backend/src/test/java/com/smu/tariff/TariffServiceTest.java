package com.smu.tariff;

import com.smu.tariff.tariff.TariffService;
import com.smu.tariff.tariff.dto.TariffRateDto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class TariffServiceTest {

    @Autowired
    private TariffService tariffService;

    @Test
    void testSearchWithValidCountryAndCategory() {
        var result = tariffService.search("SGP", "USA", "ELEC");
        assertThat(result).isNotEmpty();
    }

    @Test
    void testSearchWithUnknownCountryCodeThrows() {
        assertThrows(RuntimeException.class, () ->
            tariffService.search("XXX", "USA", "ELEC")
        );
    }

    @Test
    void testSearchReturnsEmptyList() {
        List<TariffRateDto> results = tariffService.search(null, null, null);
        assertThat(results).isEmpty();
    }
}
