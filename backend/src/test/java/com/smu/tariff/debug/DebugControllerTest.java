package com.smu.tariff.debug;

import com.smu.tariff.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DebugControllerTest {
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
    }

    @Test
    void testAuthDebug_withValidToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtService.extractUsername("validtoken")).thenReturn("user");
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);
        when(jwtService.isTokenValid("validtoken", userDetails)).thenReturn(true);

        // Mock SecurityContext
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("user");
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResponseEntity<Map<String, Object>> response = debugController.authDebug(request);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("authorizationHeader")).isEqualTo("Bearer validtoken");
        assertThat(body.get("extractedUsername")).isEqualTo("user");
        assertThat(body.get("tokenValid")).isEqualTo(true);
        assertThat(body.get("securityAuthenticationPresent")).isEqualTo(true);
        assertThat(body.get("securityPrincipal")).isEqualTo("user");
    }

    @Test
    void testSimpleDebug_noAuth() {
        when(request.getHeader("Authorization")).thenReturn(null);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResponseEntity<Map<String, Object>> response = debugController.simpleDebug(request);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("authorizationHeader")).isNull();
        assertThat(body.get("securityAuthenticationPresent")).isEqualTo(true);
        assertThat(body.get("securityPrincipalClass")).isNull();
    }
}
