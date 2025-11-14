package com.smu.tariff.debug;

import com.smu.tariff.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DebugControllerBranchTest {
    JwtService jwtService;
    UserDetailsService userDetailsService;
    DebugController debugController;
    HttpServletRequest request;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        debugController = new DebugController(jwtService, userDetailsService);
        request = mock(HttpServletRequest.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAuthDebug_noAuthHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);
        ResponseEntity<Map<String, Object>> response = debugController.authDebug(request);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().get("authorizationHeader")).isNull();
    }

    @Test
    void testAuthDebug_tokenParseError() {
        when(request.getHeader("Authorization")).thenReturn("Bearer badtoken");
        when(jwtService.extractUsername("badtoken")).thenThrow(new RuntimeException("parse fail"));
        ResponseEntity<Map<String, Object>> response = debugController.authDebug(request);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().get("tokenParseError")).isEqualTo("parse fail");
    }

    @Test
    void testAuthDebug_userLookupError() {
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtService.extractUsername("validtoken")).thenReturn("user");
        when(userDetailsService.loadUserByUsername("user")).thenThrow(new RuntimeException("user not found"));
        ResponseEntity<Map<String, Object>> response = debugController.authDebug(request);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().get("userLookupError")).isEqualTo("user not found");
    }

    @Test
    void testAuthDebug_exception() {
        when(request.getHeader("Authorization")).thenThrow(new RuntimeException("header fail"));
        ResponseEntity<Map<String, Object>> response = debugController.authDebug(request);
        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody().get("error")).isEqualTo("header fail");
    }

    @Test
    void testSimpleDebug_exception() {
        when(request.getHeader("Authorization")).thenThrow(new RuntimeException("header fail"));
        ResponseEntity<Map<String, Object>> response = debugController.simpleDebug(request);
        assertThat(response.getStatusCodeValue()).isEqualTo(500);
        assertThat(response.getBody().get("error")).isEqualTo("header fail");
    }
}
