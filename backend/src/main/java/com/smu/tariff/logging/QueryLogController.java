package com.smu.tariff.logging;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smu.tariff.user.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/query-logs")
public class QueryLogController {

    private final QueryLogRepository queryLogRepository;
    private final QueryLogService queryLogService;

    public QueryLogController(QueryLogRepository queryLogRepository,
                              QueryLogService queryLogService) {
        this.queryLogRepository = queryLogRepository;
        this.queryLogService = queryLogService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllQueryLogs() {
        List<Map<String, Object>> result = new ArrayList<>();
        User currentUser = queryLogService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }

        System.out.println("=== QueryLogController: getAllQueryLogs called for user=" + currentUser.getUsername() + " ===");

        List<QueryLog> logs = queryLogRepository.findByUserIdWithUser(currentUser.getId());
        System.out.println("Retrieved " + logs.size() + " logs from database for user=" + currentUser.getUsername());

        List<Map<String, Object>> dtoList = new ArrayList<>();
        for (QueryLog log : logs) {
            Map<String, Object> map = buildResponseMap(log);
            dtoList.add(map);

            if (dtoList.size() <= 3) {
                System.out.println("Log DTO " + map.get("id") + ": " + map.get("action") + " - " + log.getParams());
            }
        }
        result = dtoList;

        System.out.println("=== Returning " + result.size() + " logs to frontend (user=" + currentUser.getUsername() + ") ===");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getQueryLogsByUser(@PathVariable Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        User currentUser = queryLogService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }

        System.out.println("QueryLogController: getQueryLogsByUser called for userId=" + userId);
        List<QueryLog> logs = queryLogRepository.findByUserIdWithUser(userId);
        for (QueryLog log : logs) {
            result.add(buildResponseMap(log));
        }
        System.out.println("Returning " + result.size() + " logs for userId=" + userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        User currentUser = queryLogService.getCurrentUser();
        if (currentUser == null) {
            response.put("status", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        System.out.println("QueryLogController: testConnection() called for user=" + currentUser.getUsername());
        try {
            long totalLogs = queryLogRepository.countByUser_Id(currentUser.getId());
            response.put("status", "Controller is working");
            response.put("timestamp", System.currentTimeMillis());
            response.put("totalLogs", totalLogs);
            response.put("databaseConnected", true);

            System.out.println("QueryLogController: Test successful - found " + totalLogs + " logs for user=" + currentUser.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "Database query failed");
            response.put("error", e.getMessage());
            response.put("databaseConnected", false);
            System.err.println("Database test failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugDatabase() {
        Map<String, Object> response = new HashMap<>();
        User currentUser = queryLogService.getCurrentUser();
        if (currentUser == null) {
            response.put("status", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        System.out.println("QueryLogController: debugDatabase() called for user=" + currentUser.getUsername());
        try {
            List<QueryLog> logs = queryLogRepository.findByUserIdWithUser(currentUser.getId());
            response.put("totalForUser", logs.size());
            response.put("timestamp", System.currentTimeMillis());

            List<Map<String, Object>> logDetails = logs.stream()
                    .limit(5)
                    .map(log -> {
                        Map<String, Object> logInfo = new HashMap<>();
                        logInfo.put("id", log.getId());
                        logInfo.put("type", log.getType());
                        logInfo.put("params", log.getParams());
                        logInfo.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
                        logInfo.put("hasUser", log.getUser() != null);
                        if (log.getUser() != null) {
                            logInfo.put("username", log.getUser().getUsername());
                        }
                        return logInfo;
                    })
                    .collect(Collectors.toList());
            response.put("sampleLogs", logDetails);

            System.out.println("QueryLogController: Debug successful - " + logs.size() + " logs for user=" + currentUser.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            System.err.println("Debug failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/raw")
    public ResponseEntity<List<Map<String, Object>>> rawLatest() {
        List<Map<String, Object>> out = new ArrayList<>();
        User currentUser = queryLogService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(out);
        }

        try {
            List<Object[]> rows = queryLogRepository.findLatestRawByUser(currentUser.getId());
            for (Object[] r : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r[0]);
                m.put("createdAt", r[1]);
                m.put("userId", r[2]);
                out.add(m);
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            System.err.println("rawLatest error: " + e.getMessage());
            return ResponseEntity.status(500).body(out);
        }
    }

    private Map<String, Object> buildResponseMap(QueryLog log) {
        QueryLogDto dto = new QueryLogDto();
        dto.setId(log.getId());
        dto.setTimestamp(log.getCreatedAt() == null ? null : log.getCreatedAt().toString());
        dto.setType(log.getType());
        dto.setRawParams(log.getParams());
        dto.setRawResult(log.getResult());
        try {
            dto.setResultPreview(summarize(log.getResult()));
        } catch (Exception ex) {
            dto.setResultPreview("-");
            System.err.println("QueryLogController: summarize failed for log " + log.getId() + " - " + ex.getMessage());
        }

        if (log.getUser() != null) {
            dto.setUserId(log.getUser().getId());
            dto.setUser(Optional.ofNullable(log.getUser().getUsername()).orElse(""));
            dto.setUserEmail(Optional.ofNullable(log.getUser().getEmail()).orElse(""));
            try {
                dto.setUserRole(log.getUser().getRole() == null ? "" : log.getUser().getRole().name());
            } catch (Exception ex) {
                dto.setUserRole("");
            }
            dto.setUserCreatedAt(log.getUser().getCreatedAt() == null ? "" : log.getUser().getCreatedAt().toString());
        } else {
            dto.setUserId(null);
            dto.setUser("Anonymous");
            dto.setUserEmail("");
            dto.setUserRole("");
            dto.setUserCreatedAt("");
        }

        dto.setAction(log.getType());

        Map<String, String> parsed = QueryLogParamParser.parse(log.getParams());
        String origin = parsed.getOrDefault("origin", parsed.getOrDefault("from", log.getOriginCountry() != null ? log.getOriginCountry() : "-"));
        String destination = parsed.getOrDefault("destination", parsed.getOrDefault("to", log.getDestinationCountry() != null ? log.getDestinationCountry() : "-"));
        dto.setOrigin(origin);
        dto.setDestination(destination);
        dto.setCategory(parsed.getOrDefault("category", parsed.getOrDefault("cat", "-")));
        dto.setValue(parsed.getOrDefault("value", parsed.getOrDefault("val", "-")));
        dto.setDate(parsed.getOrDefault("date", "-"));

        Map<String, Object> map = new HashMap<>();
        map.put("id", dto.getId());
        map.put("createdAt", dto.getTimestamp());
        map.put("type", dto.getType());
        map.put("params", log.getParams());
        map.put("result", dto.getRawResult());
        map.put("resultPreview", dto.getResultPreview());

        map.put("userId", dto.getUserId());
        map.put("username", dto.getUser());
        map.put("userEmail", dto.getUserEmail());
        map.put("userRole", dto.getUserRole());
        map.put("userCreatedAt", dto.getUserCreatedAt());

        map.put("action", dto.getAction());
        map.put("origin", dto.getOrigin());
        map.put("destination", dto.getDestination());
        map.put("category", dto.getCategory());
        map.put("value", dto.getValue());
        map.put("date", dto.getDate());

        return map;
    }

    private String summarize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= 120) {
            return trimmed;
        }
        return trimmed.substring(0, 117) + "...";
    }
}

