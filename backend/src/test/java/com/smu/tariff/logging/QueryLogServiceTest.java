package com.smu.tariff.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smu.tariff.security.JwtService;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QueryLogServiceTest {
    @Test
    void testGetCurrentUser_fromSecurityContext_UserDetails() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("alice");
        User user = new User();
        user.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);
        User result = service.getCurrentUser();
        assertThat(result).isSameAs(user);
    }

    @Test
    void testGetCurrentUser_fromSecurityContext_usernameString() {
        User user = new User();
        user.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("irrelevant");
        when(auth.getName()).thenReturn("bob");
        SecurityContextHolder.getContext().setAuthentication(auth);
        User result = service.getCurrentUser();
        assertThat(result).isSameAs(user);
    }

    @Test
    void testGetCurrentUser_fromSecurityContext_anonymous() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("irrelevant");
        when(auth.getName()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(auth);
        User result = service.getCurrentUser();
        assertThat(result).isNull();
    }

    @Test
    void testGetCurrentUser_fromRequest_validToken() {
        // Simulate no user in security context
        SecurityContextHolder.clearContext();
        // Mock request context
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var attrs = mock(org.springframework.web.context.request.ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(attrs);
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(jwtService.extractUsername("token123")).thenReturn("carol");
        User user = new User();
        user.setUsername("carol");
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));
        User result = service.getCurrentUser();
        assertThat(result).isSameAs(user);
    }

    @Test
    void testGetCurrentUser_fromRequest_invalidToken() {
        SecurityContextHolder.clearContext();
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var attrs = mock(org.springframework.web.context.request.ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(attrs);
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(jwtService.extractUsername("token123")).thenThrow(new RuntimeException("bad token"));
        User result = service.getCurrentUser();
        assertThat(result).isNull();
    }

    @Test
    void testGetCurrentUser_fromRequest_noHeader() {
        SecurityContextHolder.clearContext();
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var attrs = mock(org.springframework.web.context.request.ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(attrs);
        when(request.getHeader("Authorization")).thenReturn(null);
        User result = service.getCurrentUser();
        assertThat(result).isNull();
    }

    @Test
    void testExtractFromParams_malformedJson() throws Exception {
        String badJson = "not-a-json";
        var m = QueryLogService.class.getDeclaredMethod("extractFromParams", String.class, String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, badJson, "origin", "from");
        assertThat(result).isNull();
    }

    @Test
    void testExtractFromParams_missingKeys() throws Exception {
        String json = "{\"foo\":\"bar\"}";
        var m = QueryLogService.class.getDeclaredMethod("extractFromParams", String.class, String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, json, "origin", "from");
        assertThat(result).isNull();
    }
    @Mock QueryLogRepository queryLogRepository;
    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock ObjectMapper objectMapper;

    QueryLogService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new QueryLogService(queryLogRepository, userRepository, jwtService, objectMapper);
    }

    @Test
    void testLogWithUser() {
        User user = new User();
        user.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(auth);
        service.log("TYPE", "params");
        verify(queryLogRepository).save(any(QueryLog.class));
    }

    @Test
    void testLogWithAnonymous() {
        SecurityContextHolder.clearContext();
        service.log("TYPE", "params");
        verify(queryLogRepository).save(any(QueryLog.class));
    }

    @Test
    void testSerializeResultTruncatesLongString() {
        String longStr = "x".repeat(5000);
        String result = invokeSerializeResult(longStr);
        assertThat(result.length()).isLessThanOrEqualTo(4096);
        assertThat(result).endsWith("…");
    }

    @Test
    void testSerializeResultHandlesException() throws Exception {
        Object badObj = new Object() {
            @Override public String toString() { return "bad"; }
        };
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("fail"));
        String result = invokeSerializeResult(badObj);
        assertThat(result).isEqualTo("bad");
    }

    private String invokeSerializeResult(Object obj) {
        try {
            var m = QueryLogService.class.getDeclaredMethod("serializeResult", Object.class);
            m.setAccessible(true);
            return (String) m.invoke(service, obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testExtractFromParamsHandlesKeys() throws Exception {
        String params = "{\"origin\":\"SG\",\"destination\":\"MY\"}";
        var m = QueryLogService.class.getDeclaredMethod("extractFromParams", String.class, String.class, String.class);
        m.setAccessible(true);
        String origin = (String) m.invoke(service, params, "origin", "from");
        String dest = (String) m.invoke(service, params, "destination", "to");
        assertThat(origin).isEqualTo("SG");
        assertThat(dest).isEqualTo("MY");
    }
}
