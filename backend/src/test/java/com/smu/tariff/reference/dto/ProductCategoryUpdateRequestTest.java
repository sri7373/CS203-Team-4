package com.smu.tariff.reference.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ProductCategoryUpdateRequestTest {
    @Test
    void testAllArgsAndFields() {
        ProductCategoryUpdateRequest req = new ProductCategoryUpdateRequest();
        req.name = "Consumer Electronics";
        req.hsCode = "8517";
        req.weightBased = Boolean.TRUE;
        req.code = "ELEC";
        assertThat(req.name).isEqualTo("Consumer Electronics");
        assertThat(req.hsCode).isEqualTo("8517");
        assertThat(req.weightBased).isTrue();
        assertThat(req.code).isEqualTo("ELEC");
    }
    @Test
    void testNullCodeAndWeightBased() {
        ProductCategoryUpdateRequest req = new ProductCategoryUpdateRequest();
        req.name = "Steel";
        req.hsCode = "7208";
        req.weightBased = null;
        req.code = null;
        assertThat(req.name).isEqualTo("Steel");
        assertThat(req.hsCode).isEqualTo("7208");
        assertThat(req.weightBased).isNull();
        assertThat(req.code).isNull();
    }
}
