package com.smu.tariff.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class SequenceInitializer implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Reset sequence to MAX(id) in the table
        jdbcTemplate.execute(
            "SELECT setval('tariff_rate_id_seq', (SELECT COALESCE(MAX(id),0) FROM tariff_rate))"
        );
        System.out.println("tariff_rate_id_seq synchronized with max(id)");
    }
}