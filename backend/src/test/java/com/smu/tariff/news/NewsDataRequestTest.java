package com.smu.tariff.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class NewsDataRequestTest {
    @Test
    void testBuilderAndGettersSetters() {
        NewsDataRequest req = NewsDataRequest.builder()
                .query("tariff")
                .queryInTitle("title")
                .queryInMeta("meta")
                .country(List.of("us", "sg"))
                .category(List.of("business"))
                .language(List.of("en"))
                .domain(List.of("cnn"))
                .domainUrl(List.of("cnn.com"))
                .fromDate("2020-01-01")
                .toDate("2020-12-31")
                .timeframe("24h")
                .sentiment("positive")
                .aiTag(List.of("ai"))
                .fullContent(true)
                .image(false)
                .video(true)
                .removeDuplicate(true)
                .priorityDomain("top")
                .size(10)
                .page("token")
                .build();

        assertThat(req.getQuery()).isEqualTo("tariff");
        assertThat(req.getQueryInTitle()).isEqualTo("title");
        assertThat(req.getQueryInMeta()).isEqualTo("meta");
        assertThat(req.getCountry()).containsExactly("us", "sg");
        assertThat(req.getCategory()).containsExactly("business");
        assertThat(req.getLanguage()).containsExactly("en");
        assertThat(req.getDomain()).containsExactly("cnn");
        assertThat(req.getDomainUrl()).containsExactly("cnn.com");
        assertThat(req.getFromDate()).isEqualTo("2020-01-01");
        assertThat(req.getToDate()).isEqualTo("2020-12-31");
        assertThat(req.getTimeframe()).isEqualTo("24h");
        assertThat(req.getSentiment()).isEqualTo("positive");
        assertThat(req.getAiTag()).containsExactly("ai");
        assertThat(req.getFullContent()).isTrue();
        assertThat(req.getImage()).isFalse();
        assertThat(req.getVideo()).isTrue();
        assertThat(req.getRemoveDuplicate()).isTrue();
        assertThat(req.getPriorityDomain()).isEqualTo("top");
        assertThat(req.getSize()).isEqualTo(10);
        assertThat(req.getPage()).isEqualTo("token");

        // Test setters
        req.setQuery("q");
        req.setQueryInTitle("qt");
        req.setQueryInMeta("qm");
        req.setCountry(List.of("fr"));
        req.setCategory(List.of("cat"));
        req.setLanguage(List.of("fr"));
        req.setDomain(List.of("bbc"));
        req.setDomainUrl(List.of("bbc.com"));
        req.setFromDate("2021-01-01");
        req.setToDate("2021-12-31");
        req.setTimeframe("48h");
        req.setSentiment("neutral");
        req.setAiTag(List.of("tag"));
        req.setFullContent(false);
        req.setImage(true);
        req.setVideo(false);
        req.setRemoveDuplicate(false);
        req.setPriorityDomain("low");
        req.setSize(5);
        req.setPage("p2");

        assertThat(req.getQuery()).isEqualTo("q");
        assertThat(req.getQueryInTitle()).isEqualTo("qt");
        assertThat(req.getQueryInMeta()).isEqualTo("qm");
        assertThat(req.getCountry()).containsExactly("fr");
        assertThat(req.getCategory()).containsExactly("cat");
        assertThat(req.getLanguage()).containsExactly("fr");
        assertThat(req.getDomain()).containsExactly("bbc");
        assertThat(req.getDomainUrl()).containsExactly("bbc.com");
        assertThat(req.getFromDate()).isEqualTo("2021-01-01");
        assertThat(req.getToDate()).isEqualTo("2021-12-31");
        assertThat(req.getTimeframe()).isEqualTo("48h");
        assertThat(req.getSentiment()).isEqualTo("neutral");
        assertThat(req.getAiTag()).containsExactly("tag");
        assertThat(req.getFullContent()).isFalse();
        assertThat(req.getImage()).isTrue();
        assertThat(req.getVideo()).isFalse();
        assertThat(req.getRemoveDuplicate()).isFalse();
        assertThat(req.getPriorityDomain()).isEqualTo("low");
        assertThat(req.getSize()).isEqualTo(5);
        assertThat(req.getPage()).isEqualTo("p2");
    }
}
