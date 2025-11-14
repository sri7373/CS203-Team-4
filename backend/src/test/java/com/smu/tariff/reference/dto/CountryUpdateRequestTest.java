package com.smu.tariff.reference.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CountryUpdateRequestTest {
    @Test
    void testAllArgsAndFields() {
        CountryUpdateRequest req = new CountryUpdateRequest();
        req.name = "Malaysia";
        req.code = "MYS";
        assertThat(req.name).isEqualTo("Malaysia");
        assertThat(req.code).isEqualTo("MYS");
    }
    @Test
    void testNullCodeAllowed() {
        CountryUpdateRequest req = new CountryUpdateRequest();
        req.name = "Thailand";
        req.code = null;
        assertThat(req.name).isEqualTo("Thailand");
        assertThat(req.code).isNull();
    }
}
