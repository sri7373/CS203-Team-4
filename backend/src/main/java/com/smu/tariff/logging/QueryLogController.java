package com.smu.tariff.logging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/query-logs")
public class QueryLogController {

    @Autowired
    private QueryLogRepository queryLogRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllQueryLogs() {
        System.out.println("=== QueryLogController: getAllQueryLogs called ===");
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            long count = queryLogRepository.count();
            System.out.println("Total logs in database: " + count);
            
            if (count == 0) {
                System.out.println("Database is empty - returning empty list");
                return ResponseEntity.ok(result);
            }
            
            // Get all logs sorted by newest first so frontend sees most recent entries first
            // Use a fetch join to load the user relationship and avoid lazy init errors
            List<QueryLog> logs = queryLogRepository.findAllWithUser();
            System.out.println("Retrieved " + logs.size() + " logs from database (sorted by createdAt desc)");

            // Build typed DTOs with requested columns: Timestamp, User, Action, Origin, Destination, Category, Value, Date
            List<Map<String, Object>> dtoList = new ArrayList<>();
            for (QueryLog log : logs) {
                QueryLogDto dto = new QueryLogDto();
                // fill raw DB values
                dto.setId(log.getId());
                dto.setTimestamp(log.getCreatedAt() == null ? null : log.getCreatedAt().toString());
                dto.setType(log.getType());
                dto.setRawParams(log.getParams());

                // fill user info if present
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

                // set action (type is also included raw)
                dto.setAction(log.getType());

                // parse params into semantic fields
                Map<String, String> parsed = QueryLogParamParser.parse(log.getParams());
                dto.setOrigin(parsed.getOrDefault("origin", parsed.getOrDefault("from", "-")));
                dto.setDestination(parsed.getOrDefault("destination", parsed.getOrDefault("to", "-")));
                dto.setCategory(parsed.getOrDefault("category", parsed.getOrDefault("cat", "-")));
                dto.setValue(parsed.getOrDefault("value", parsed.getOrDefault("val", "-")));
                dto.setDate(parsed.getOrDefault("date", "-"));

                Map<String, Object> map = new HashMap<>();
                // Raw DB columns
                map.put("id", dto.getId());
                map.put("createdAt", dto.getTimestamp());
                map.put("type", dto.getType());
                map.put("params", parsed.isEmpty() ? log.getParams() : log.getParams());

                // User columns (from joined user, may be null)
                map.put("userId", dto.getUserId());
                map.put("username", dto.getUser());
                map.put("userEmail", dto.getUserEmail());
                map.put("userRole", dto.getUserRole());
                map.put("userCreatedAt", dto.getUserCreatedAt());

                // Parsed & friendly columns
                map.put("action", dto.getAction());
                map.put("origin", dto.getOrigin());
                map.put("destination", dto.getDestination());
                map.put("category", dto.getCategory());
                map.put("value", dto.getValue());
                map.put("date", dto.getDate());

                dtoList.add(map);

                if (dtoList.size() <= 3) {
                    System.out.println("Log DTO " + dto.getId() + ": " + dto.getAction() + " - " + log.getParams() + " (user=" + dto.getUser() + ")");
                }
            }
            result = dtoList;
            
            System.out.println("=== Returning " + result.size() + " logs to frontend ===");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.err.println("=== ERROR in getAllQueryLogs: " + e.getMessage() + " ===");
            return ResponseEntity.ok(result); // Return empty list on error
        }
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        System.out.println("QueryLogController: testConnection() called");
        
        Map<String, Object> response = new HashMap<>();
        try {
            long totalLogs = queryLogRepository.count();
            response.put("status", "Controller is working");
            response.put("timestamp", System.currentTimeMillis());
            response.put("totalLogs", totalLogs);
            response.put("databaseConnected", true);
            
            System.out.println("QueryLogController: Test successful - found " + totalLogs + " logs in database");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "Database connection failed");
            response.put("error", e.getMessage());
            response.put("databaseConnected", false);
            System.err.println("Database test failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugDatabase() {
        System.out.println("QueryLogController: debugDatabase() called");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // Check total count
            long total = queryLogRepository.count();
            
            // Try to get all logs (even with null users) and sort newest first so debug shows latest activity
            List<QueryLog> allLogs = queryLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            
            response.put("totalInDatabase", total);
            response.put("allLogsCount", allLogs.size());
            response.put("timestamp", System.currentTimeMillis());
            
            // Show details of first few logs
            List<Map<String, Object>> logDetails = new ArrayList<>();
            for (int i = 0; i < Math.min(5, allLogs.size()); i++) {
                QueryLog log = allLogs.get(i);
                Map<String, Object> logInfo = new HashMap<>();
                logInfo.put("id", log.getId());
                logInfo.put("type", log.getType());
                logInfo.put("params", log.getParams());
                logInfo.put("createdAt", log.getCreatedAt().toString());
                logInfo.put("hasUser", log.getUser() != null);
                if (log.getUser() != null) {
                    logInfo.put("username", log.getUser().getUsername());
                }
                logDetails.add(logInfo);
            }
            response.put("sampleLogs", logDetails);
            
            System.out.println("QueryLogController: Debug successful - " + total + " total logs");
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
        try {
            List<Object[]> rows = queryLogRepository.findLatestRaw();
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
}