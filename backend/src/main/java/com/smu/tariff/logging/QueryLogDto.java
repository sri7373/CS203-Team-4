package com.smu.tariff.logging;

import java.time.Instant;

public class QueryLogDto {
    private Long id;
    private String timestamp; // ISO string
    // Raw DB fields
    private String type;
    private String rawParams;

    // User info
    private Long userId;
    private String user;
    private String userEmail;
    private String userRole;
    private String userCreatedAt;

    // Parsed semantic fields
    private String action;
    private String origin;
    private String destination;
    private String category;
    private String value;
    private String date; // effective date

    public QueryLogDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant instant) {
        if (instant == null) this.timestamp = null;
        else this.timestamp = instant.toString();
    }

    public void setTimestamp(String ts) { this.timestamp = ts; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRawParams() { return rawParams; }
    public void setRawParams(String rawParams) { this.rawParams = rawParams; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getUserCreatedAt() { return userCreatedAt; }
    public void setUserCreatedAt(String userCreatedAt) { this.userCreatedAt = userCreatedAt; }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
