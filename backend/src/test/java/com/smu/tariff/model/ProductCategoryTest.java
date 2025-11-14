package com.smu.tariff.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProductCategoryTest {
    @Test
    void testGettersAndSetters() {
        ProductCategory cat = new ProductCategory();
        cat.setCode("ELEC");
        cat.setName("Electronics");
        cat.setHsCode("1234.56");
        cat.setWeightBased(true);
        assertThat(cat.getCode()).isEqualTo("ELEC");
        assertThat(cat.getName()).isEqualTo("Electronics");
        assertThat(cat.getHsCode()).isEqualTo("1234.56");
        assertThat(cat.getWeightBased()).isTrue();
    }

    @Test
    void testAllArgsConstructor() {
        ProductCategory cat = new ProductCategory("ELEC", "Electronics", "1234.56", true);
        assertThat(cat.getCode()).isEqualTo("ELEC");
        assertThat(cat.getName()).isEqualTo("Electronics");
        assertThat(cat.getHsCode()).isEqualTo("1234.56");
        assertThat(cat.getWeightBased()).isTrue();
    }

    @Test
    void testTwoArgConstructor() {
        ProductCategory cat = new ProductCategory("ELEC", "Electronics");
        assertThat(cat.getCode()).isEqualTo("ELEC");
        assertThat(cat.getName()).isEqualTo("Electronics");
        assertThat(cat.getHsCode()).isNull();
        assertThat(cat.getWeightBased()).isFalse();
    }

    @Test
    void testSetWeightBasedNull() {
        ProductCategory cat = new ProductCategory();
        cat.setWeightBased(null);
        assertThat(cat.getWeightBased()).isFalse();
    }
}
