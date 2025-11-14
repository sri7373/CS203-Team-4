package com.smu.tariff;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class RootControllerTest {
    @Test
    void testRedirectToSwagger() {
        RootController controller = new RootController();
        String result = controller.redirectToSwagger();
        assertThat(result).isEqualTo("redirect:/swagger-ui/index.html");
    }
}
