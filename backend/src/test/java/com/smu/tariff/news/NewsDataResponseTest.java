package com.smu.tariff.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class NewsDataResponseTest {
    @Test
    void testGettersAndSetters() {
        NewsDataResponse resp = new NewsDataResponse();
        resp.setStatus("ok");
        resp.setTotalResults(5);
        resp.setNextPage("token");
        NewsArticle article = new NewsArticle();
        resp.setArticles(List.of(article));

        assertThat(resp.getStatus()).isEqualTo("ok");
        assertThat(resp.getTotalResults()).isEqualTo(5);
        assertThat(resp.getNextPage()).isEqualTo("token");
        assertThat(resp.getArticles()).containsExactly(article);
    }
}
