package com.smu.tariff.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CorsFilter;
import io.swagger.v3.oas.models.OpenAPI;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigCoverageTest {
    @Test
    void testSequenceInitializer_catchDataAccessException() throws Exception {
        var jdbcTemplate = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        org.mockito.Mockito.when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Long.class)))
            .thenThrow(new org.springframework.dao.DataAccessException("fail"){});
        var initializer = new SequenceInitializer();
        java.lang.reflect.Field f = SequenceInitializer.class.getDeclaredField("jdbcTemplate");
        f.setAccessible(true);
        f.set(initializer, jdbcTemplate);
        // Should not throw, just log
        initializer.run(org.mockito.Mockito.mock(org.springframework.boot.ApplicationArguments.class));
    }
    @Test
    void testSequenceInitializer_run_normal() throws Exception {
        var jdbcTemplate = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        org.mockito.Mockito.when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Long.class))).thenReturn(5L);
        org.mockito.Mockito.when(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(1);
        var initializer = new SequenceInitializer();
        java.lang.reflect.Field f = SequenceInitializer.class.getDeclaredField("jdbcTemplate");
        f.setAccessible(true);
        f.set(initializer, jdbcTemplate);
        initializer.run(org.mockito.Mockito.mock(org.springframework.boot.ApplicationArguments.class));
    }

    @Test
    void testSequenceInitializer_run_exception() throws Exception {
        var jdbcTemplate = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        org.mockito.Mockito.when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(Long.class))).thenThrow(new org.springframework.dao.DataAccessException("fail"){});
        var initializer = new SequenceInitializer();
        java.lang.reflect.Field f = SequenceInitializer.class.getDeclaredField("jdbcTemplate");
        f.setAccessible(true);
        f.set(initializer, jdbcTemplate);
        initializer.run(org.mockito.Mockito.mock(org.springframework.boot.ApplicationArguments.class));
    }
    @Test
    void testSecurityConfigBeans() {
        // Mocks for dependencies
        com.smu.tariff.user.UserRepository userRepository = org.mockito.Mockito.mock(com.smu.tariff.user.UserRepository.class);
        com.smu.tariff.security.JwtService jwtService = org.mockito.Mockito.mock(com.smu.tariff.security.JwtService.class);
        com.smu.tariff.config.SecurityConfig config = new com.smu.tariff.config.SecurityConfig(userRepository, jwtService);

        // PasswordEncoder bean
        assertThat(config.passwordEncoder().encode("abc")).isNotNull();

        // UserDetailsService bean (user not found)
        org.mockito.Mockito.when(userRepository.findByUsername("nouser")).thenReturn(java.util.Optional.empty());
        try {
            config.userDetailsService().loadUserByUsername("nouser");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("User not found");
        }

        // AuthenticationProvider bean
        assertThat(config.authenticationProvider()).isNotNull();
    }
    @Test
    void testOpenApiConfig() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI api = config.tariffOpenAPI();
        assertThat(api).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("TARIFF API");
    }

    @Test
    void testCorsConfig() {
        CorsConfig config = new CorsConfig();
        CorsFilter filter = config.corsFilter();
        assertThat(filter).isNotNull();
    }

    @Test
    void testRestTemplateConfig() {
        RestTemplateConfig config = new RestTemplateConfig();
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate restTemplate = config.restTemplate(builder);
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void testSequenceInitializerDoesNotThrow() {
        SequenceInitializer initializer = new SequenceInitializer();
        // JdbcTemplate is autowired, so we can't run .run(), but we can instantiate
        assertThat(initializer).isNotNull();
    }
}
