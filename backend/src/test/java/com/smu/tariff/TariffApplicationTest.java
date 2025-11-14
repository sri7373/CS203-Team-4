package com.smu.tariff;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TariffApplicationTest {
    @Test
    void testMainMethodExists() throws Exception {
        // Check that the main method exists and is public static void
        java.lang.reflect.Method main = TariffApplication.class.getMethod("main", String[].class);
        assertThat(main).isNotNull();
        assertThat(java.lang.reflect.Modifier.isStatic(main.getModifiers())).isTrue();
        assertThat(main.getReturnType()).isEqualTo(void.class);
    }
}
