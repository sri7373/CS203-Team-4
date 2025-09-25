package com.smu.tariff.logging;

import com.smu.tariff.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "query_log")
public class QueryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 32)
    private String type; // CALCULATE or SEARCH

    @Column(nullable = false, length = 2048)
    private String params;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public QueryLog() { }

    public QueryLog(User user, String type, String params) {
        this.user = user;
        this.type = type;
        this.params = params;
    }

    // Getters used by controllers and serializers
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public String getParams() {
        return params;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
