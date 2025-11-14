package com.smu.tariff.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

class JwtServiceTest {
    JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Use a 256-bit (32-byte) base64-encoded secret for HS256
        // Generate a random 32-byte key and encode as base64: e.g., '0123456789abcdef0123456789abcdef' (hex) is not base64.
        // Example base64 for 32 bytes: 'MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVm'
        jwtService.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVm");
        jwtService.setJwtExpirationMs(10000L);
    }

    @Test
    void testGenerateAndValidateToken() {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("user");
        // Use a collection of GrantedAuthority
    java.util.Collection<? extends GrantedAuthority> authorities = java.util.Collections.singletonList((GrantedAuthority) () -> "ROLE_USER");
    when(user.getAuthorities()).thenAnswer(invocation -> authorities);
        String token = jwtService.generateToken(user);
        assertThat(token).isNotNull();
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user");
    }

    @Test
    void testExtractClaim() {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("user");
        when(user.getAuthorities()).thenReturn(Collections.emptyList());
        String token = jwtService.generateToken(user);
        String subject = jwtService.extractClaim(token, Claims::getSubject);
        assertThat(subject).isEqualTo("user");
    }

    @Test
    void testIsTokenExpired() throws InterruptedException {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn("user");
        when(user.getAuthorities()).thenReturn(Collections.emptyList());
        jwtService.setJwtExpirationMs(10L); // 10 ms expiration
        String token = jwtService.generateToken(user);
        Thread.sleep(20); // Wait for token to expire
        // isTokenValid will internally call extractUsername, which may throw ExpiredJwtException
        boolean expired;
        try {
            expired = !jwtService.isTokenValid(token, user);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            expired = true;
        }
        assertThat(expired).isTrue();
    }
}
