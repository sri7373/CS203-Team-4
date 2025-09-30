package com.smu.tariff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SequenceInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Reset sequence to MAX(id) in the table
        jdbcTemplate.execute(
            "SELECT setval('tariff_rate_id_seq', (SELECT COALESCE(MAX(id),0) FROM tariff_rate))"
        );
        logger.info("tariff_rate_id_seq synchronized with max(id)");
    }
}