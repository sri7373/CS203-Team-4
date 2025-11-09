package com.smu.tariff;

import com.smu.tariff.tariff.TariffService;
import com.smu.tariff.tariff.dto.TariffRateDto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class TariffServiceTest {

    @Autowired
    private TariffService tariffService;

    @Test
    @Sql(scripts = {"/cleanup.sql", "/test-data.sql"})
    void testSearchWithValidCountryAndCategory() {
        var result = tariffService.search("SGP", "USA", "ELEC");
        assertThat(result).isNotNull();
    }

    @Test
    @Sql(scripts = {"/cleanup.sql", "/test-data.sql"})
    void testSearchWithUnknownCountryCodeThrows() {
        assertThrows(RuntimeException.class, () ->
            tariffService.search("XXX", "USA", "ELEC")
        );
    }

    @Test
    @Sql(scripts = {"/cleanup.sql", "/test-data.sql"})
    void testSearchReturnsResultsWhenNoFilterApplied() {
        List<TariffRateDto> results = tariffService.search(null, null, null);
        assertThat(results).isNotEmpty();
    }
}
