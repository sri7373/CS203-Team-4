package com.smu.tariff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SequenceInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long maxId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM tariff_rate",
                Long.class
            );

            long sequenceValue = (maxId == null || maxId < 1) ? 1L : maxId;
            boolean isCalled = maxId != null && maxId >= 1;

            jdbcTemplate.update(
                "SELECT setval('tariff_rate_id_seq', ?, ?)",
                sequenceValue,
                isCalled
            );
            logger.info("tariff_rate_id_seq synchronized with value {}", sequenceValue);
        } catch (DataAccessException ex) {
            logger.warn(
                "Skipping tariff_rate_id_seq initialization: {}",
                ex.getMessage()
            );
        }
    }
}
