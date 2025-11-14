package com.smu.tariff.logging;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class QueryLogParamParserTest {
    @Test
    void testParse_null() {
        assertThat(QueryLogParamParser.parse(null)).isEmpty();
    }

    @Test
    void testParse_empty() {
        assertThat(QueryLogParamParser.parse("")).isEmpty();
        assertThat(QueryLogParamParser.parse("   ")).isEmpty();
    }

    @Test
    void testParse_json() {
        Map<String, String> result = QueryLogParamParser.parse("{\"foo\":\"bar\",\"baz\":123}");
        assertThat(result).containsEntry("foo", "bar");
        assertThat(result).containsEntry("baz", "123");
    }

    @Test
    void testParse_bracesAndColon() {
        Map<String, String> result = QueryLogParamParser.parse("{foo:bar, baz:qux}");
        assertThat(result).containsEntry("foo", "bar");
        assertThat(result).containsEntry("baz", "qux");
    }

    @Test
    void testParse_noBraces() {
        Map<String, String> result = QueryLogParamParser.parse("foo:bar, baz:qux");
        assertThat(result).containsEntry("foo", "bar");
        assertThat(result).containsEntry("baz", "qux");
    }

    @Test
    void testParse_invalidJson() {
        Map<String, String> result = QueryLogParamParser.parse("foo:bar, baz:qux");
        assertThat(result).containsEntry("foo", "bar");
        assertThat(result).containsEntry("baz", "qux");
    }

    @Test
    void testParse_partialPairs() {
        Map<String, String> result = QueryLogParamParser.parse("foo:bar, baz");
        assertThat(result).containsEntry("foo", "bar");
        assertThat(result).doesNotContainKey("baz");
    }
}
