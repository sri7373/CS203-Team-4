package com.smu.tariff.logging;

import java.time.Instant;

import com.smu.tariff.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "origin_country", length = 16)
    private String originCountry;

    @Column(name = "destination_country", length = 16)
    private String destinationCountry;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public QueryLog() { }

    public QueryLog(User user, String type, String params) {
        this(user, type, params, null, null, null);
    }

    public QueryLog(User user, String type, String params, String result) {
        this(user, type, params, result, null, null);
    }

    public QueryLog(User user, String type, String params, String result, String originCountry, String destinationCountry) {
        this.user = user;
        this.type = type;
        this.params = params;
        this.result = result;
        this.originCountry = originCountry;
        this.destinationCountry = destinationCountry;
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

    public String getResult() {
        return result;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
