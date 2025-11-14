package com.smu.tariff.reference.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ProductCategoryCreateRequestTest {
    @Test
    void testAllArgsAndFields() {
        ProductCategoryCreateRequest req = new ProductCategoryCreateRequest();
        req.code = "ELEC";
        req.name = "Electronics";
        req.hsCode = "8517";
        req.weightBased = true;
        assertThat(req.code).isEqualTo("ELEC");
        assertThat(req.name).isEqualTo("Electronics");
        assertThat(req.hsCode).isEqualTo("8517");
        assertThat(req.weightBased).isTrue();
    }
    @Test
    void testWeightBasedFalse() {
        ProductCategoryCreateRequest req = new ProductCategoryCreateRequest();
        req.weightBased = false;
        assertThat(req.weightBased).isFalse();
    }
}
