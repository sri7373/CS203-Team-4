package com.smu.tariff.reference.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CountryCreateRequestTest {
    @Test
    void testAllArgsAndFields() {
        CountryCreateRequest req = new CountryCreateRequest();
        req.code = "SGP";
        req.name = "Singapore";
        assertThat(req.code).isEqualTo("SGP");
        assertThat(req.name).isEqualTo("Singapore");
    }
}
