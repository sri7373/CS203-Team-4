package com.smu.tariff.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smu.tariff.user.User;
import com.smu.tariff.user.UserRepository;
import com.smu.tariff.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryLogServiceUnitTest {

    private QueryLogRepository queryLogRepository;
    private UserRepository userRepository;
    private JwtService jwtService;
    private ObjectMapper mapper;
    private QueryLogService service;

    @BeforeEach
    void setUp() {
        queryLogRepository = mock(QueryLogRepository.class);
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        mapper = new ObjectMapper();
        service = new QueryLogService(queryLogRepository, userRepository, jwtService, mapper);
    }

    @Test
    void log_shouldSaveQueryLog_withAuthenticatedUser() {
        User user = new User("testuser", "t@e.com", "pw", com.smu.tariff.user.Role.USER);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);

        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        service.log("CALC", "{\"origin\":\"SGP\"}");

        ArgumentCaptor<QueryLog> cap = ArgumentCaptor.forClass(QueryLog.class);
        verify(queryLogRepository).save(cap.capture());
        QueryLog saved = cap.getValue();
        assertEquals("CALC", saved.getType());
        assertEquals("{\"origin\":\"SGP\"}", saved.getParams());
        assertNotNull(saved.getCreatedAt());
        assertEquals("testuser", saved.getUser().getUsername());
    }
}
