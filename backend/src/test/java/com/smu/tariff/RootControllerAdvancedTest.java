package com.smu.tariff;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.smu.tariff.security.JwtService;
import com.smu.tariff.security.JwtAuthFilter;

@WebMvcTest(RootController.class)
@AutoConfigureMockMvc(addFilters = false)
class RootControllerAdvancedTest {
    @Autowired
    private MockMvc mockMvc;

    // Mock security beans so context loads
    @MockBean
    private JwtService jwtService;
    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    void testRootRedirectsToSwagger() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/swagger-ui/index.html"));
    }

    // Removed testNonRootReturns404 as requested
}
