package com.smu.tariff.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceUnitTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        // create a 32-byte key and set as base64 so signing works in tests
        String raw = "01234567890123456789012345678901"; // 32 chars
        String base64 = Base64.getEncoder().encodeToString(raw.getBytes());

        Field secretField = JwtService.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtService, base64);

        Field expField = JwtService.class.getDeclaredField("jwtExpirationMs");
        expField.setAccessible(true);
        expField.setLong(jwtService, 3600000L);
    }

    @Test
    void generateAndValidateToken() {
        UserDetails user = new User("alice", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertEquals("alice", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }
}
