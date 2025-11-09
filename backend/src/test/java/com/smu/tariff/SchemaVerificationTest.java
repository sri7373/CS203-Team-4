package com.smu.tariff;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SchemaVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void queryLogTableContainsResultAndRouteColumns() throws Exception {
        Set<String> expected = Set.of("RESULT", "ORIGIN_COUNTRY", "DESTINATION_COUNTRY");
        Set<String> found = new HashSet<>();

        try (ResultSet rs = dataSource.getConnection()
                .getMetaData()
                .getColumns(null, null, "QUERY_LOG", null)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                if (expected.contains(column)) {
                    found.add(column);
                }
            }
        }

        assertThat(found)
            .as("query_log table should contain result/origin_country/destination_country columns")
            .isEqualTo(expected);
    }
}
