package com.smu.tariff.logging;

import com.smu.tariff.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryLogControllerTest {
    QueryLogRepository repo;
    QueryLogService service;
    QueryLogController controller;
    User user;

    @BeforeEach
    void setUp() {
        repo = mock(QueryLogRepository.class);
        service = mock(QueryLogService.class);
        controller = new QueryLogController(repo, service);
        user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn("testuser");
    }

    @Test
    void testGetAllQueryLogs_unauthorized() {
        when(service.getCurrentUser()).thenReturn(null);
        ResponseEntity<List<Map<String, Object>>> resp = controller.getAllQueryLogs();
    assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    void testGetAllQueryLogs_success() {
        when(service.getCurrentUser()).thenReturn(user);
        QueryLog log = mock(QueryLog.class);
        when(log.getId()).thenReturn(1L);
    when(log.getCreatedAt()).thenReturn(java.time.Instant.now());
        when(log.getType()).thenReturn("ACTION");
        when(log.getParams()).thenReturn("foo:bar");
        when(log.getResult()).thenReturn("result");
        when(log.getUser()).thenReturn(user);
        when(repo.findByUserIdWithUser(1L)).thenReturn(List.of(log));
        ResponseEntity<List<Map<String, Object>>> resp = controller.getAllQueryLogs();
    assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0)).containsEntry("id", 1L);
    }

    @Test
    void testGetQueryLogsByUser_unauthorized() {
        when(service.getCurrentUser()).thenReturn(null);
        ResponseEntity<List<Map<String, Object>>> resp = controller.getQueryLogsByUser(1L);
    assertThat(resp.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void testGetQueryLogsByUser_forbidden() {
        User other = mock(User.class);
        when(other.getId()).thenReturn(2L);
        when(service.getCurrentUser()).thenReturn(other);
        ResponseEntity<List<Map<String, Object>>> resp = controller.getQueryLogsByUser(1L);
    assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void testGetQueryLogsByUser_success() {
        when(service.getCurrentUser()).thenReturn(user);
        QueryLog log = mock(QueryLog.class);
        when(log.getId()).thenReturn(2L);
    when(log.getCreatedAt()).thenReturn(java.time.Instant.now());
        when(log.getType()).thenReturn("ACTION");
        when(log.getParams()).thenReturn("foo:bar");
        when(log.getResult()).thenReturn("result");
        when(log.getUser()).thenReturn(user);
        when(repo.findByUserIdWithUser(1L)).thenReturn(List.of(log));
        ResponseEntity<List<Map<String, Object>>> resp = controller.getQueryLogsByUser(1L);
    assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0)).containsEntry("id", 2L);
    }

    @Test
    void testTestConnection_unauthorized() {
        when(service.getCurrentUser()).thenReturn(null);
        ResponseEntity<Map<String, Object>> resp = controller.testConnection();
    assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).containsEntry("status", "Unauthorized");
    }

    @Test
    void testTestConnection_success() {
        when(service.getCurrentUser()).thenReturn(user);
        when(repo.countByUser_Id(1L)).thenReturn(5L);
        ResponseEntity<Map<String, Object>> resp = controller.testConnection();
    assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("status", "Controller is working");
        assertThat(resp.getBody()).containsEntry("totalLogs", 5L);
        assertThat(resp.getBody()).containsEntry("databaseConnected", true);
    }

    @Test
    void testTestConnection_dbError() {
        when(service.getCurrentUser()).thenReturn(user);
        when(repo.countByUser_Id(1L)).thenThrow(new RuntimeException("fail"));
        ResponseEntity<Map<String, Object>> resp = controller.testConnection();
    assertThat(resp.getStatusCode().value()).isEqualTo(500);
        assertThat(resp.getBody()).containsEntry("status", "Database query failed");
        assertThat(resp.getBody()).containsEntry("databaseConnected", false);
    }

    @Test
    void testDebugDatabase_unauthorized() {
        when(service.getCurrentUser()).thenReturn(null);
        ResponseEntity<Map<String, Object>> resp = controller.debugDatabase();
    assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).containsEntry("status", "Unauthorized");
    }

    @Test
    void testDebugDatabase_success() {
        when(service.getCurrentUser()).thenReturn(user);
        QueryLog log = mock(QueryLog.class);
        when(log.getId()).thenReturn(3L);
    when(log.getCreatedAt()).thenReturn(java.time.Instant.now());
        when(log.getType()).thenReturn("ACTION");
        when(log.getParams()).thenReturn("foo:bar");
        when(log.getResult()).thenReturn("result");
        when(log.getUser()).thenReturn(user);
        when(repo.findByUserIdWithUser(1L)).thenReturn(List.of(log));
        ResponseEntity<Map<String, Object>> resp = controller.debugDatabase();
    assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsKey("totalForUser");
        assertThat(resp.getBody()).containsKey("sampleLogs");
    }

    @Test
    void testDebugDatabase_error() {
        when(service.getCurrentUser()).thenReturn(user);
        when(repo.findByUserIdWithUser(1L)).thenThrow(new RuntimeException("fail"));
        ResponseEntity<Map<String, Object>> resp = controller.debugDatabase();
    assertThat(resp.getStatusCode().value()).isEqualTo(500);
        assertThat(resp.getBody()).containsEntry("error", "fail");
        assertThat(resp.getBody()).containsEntry("errorType", "RuntimeException");
    }

    @Test
    void testRawLatest_unauthorized() {
        when(service.getCurrentUser()).thenReturn(null);
        ResponseEntity<List<Map<String, Object>>> resp = controller.rawLatest();
    assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    void testRawLatest_success() {
        when(service.getCurrentUser()).thenReturn(user);
    Object[] row = new Object[]{1L, java.time.Instant.now(), 1L};
    List<Object[]> rows = new ArrayList<>();
    rows.add(row);
    when(repo.findLatestRawByUser(1L)).thenReturn(rows);
        ResponseEntity<List<Map<String, Object>>> resp = controller.rawLatest();
    assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0)).containsEntry("id", 1L);
    }

    @Test
    void testRawLatest_error() {
        when(service.getCurrentUser()).thenReturn(user);
        when(repo.findLatestRawByUser(1L)).thenThrow(new RuntimeException("fail"));
        ResponseEntity<List<Map<String, Object>>> resp = controller.rawLatest();
    assertThat(resp.getStatusCode().value()).isEqualTo(500);
        assertThat(resp.getBody()).isEmpty();
    }
}
