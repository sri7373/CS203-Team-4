package com.smu.tariff.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class NewsArticleTest {
    @Test
    void testGettersAndSetters() {
        NewsArticle article = new NewsArticle();
        article.setArticleId("id");
        article.setTitle("title");
        article.setLink("link");
        article.setDescription("desc");
        article.setContent("content");
        article.setPubDate("2020-01-01");
        article.setPubDateTZ("+08:00");
        article.setImageUrl("img");
        article.setVideoUrl("vid");
        article.setSourceId("sid");
        article.setSourceUrl("surl");
        article.setSourceIcon("icon");
        article.setSourcePriority(1);
        article.setLanguage("en");
        article.setCountry(List.of("us"));
        article.setCategory(List.of("cat"));
        article.setCreator(List.of("me"));
        article.setKeywords(List.of("kw"));
        article.setAiTag("tag");
        article.setSentiment("pos");
        article.setSentimentStats("stats");
        article.setAiRegion("region");
        article.setAiOrg("org");
        article.setAiSummary("summary");
        article.setAiContent("aicontent");
        article.setDuplicate(true);

        assertThat(article.getArticleId()).isEqualTo("id");
        assertThat(article.getTitle()).isEqualTo("title");
        assertThat(article.getLink()).isEqualTo("link");
        assertThat(article.getDescription()).isEqualTo("desc");
        assertThat(article.getContent()).isEqualTo("content");
        assertThat(article.getPubDate()).isEqualTo("2020-01-01");
        assertThat(article.getPubDateTZ()).isEqualTo("+08:00");
        assertThat(article.getImageUrl()).isEqualTo("img");
        assertThat(article.getVideoUrl()).isEqualTo("vid");
        assertThat(article.getSourceId()).isEqualTo("sid");
        assertThat(article.getSourceUrl()).isEqualTo("surl");
        assertThat(article.getSourceIcon()).isEqualTo("icon");
        assertThat(article.getSourcePriority()).isEqualTo(1);
        assertThat(article.getLanguage()).isEqualTo("en");
        assertThat(article.getCountry()).containsExactly("us");
        assertThat(article.getCategory()).containsExactly("cat");
        assertThat(article.getCreator()).containsExactly("me");
        assertThat(article.getKeywords()).containsExactly("kw");
        assertThat(article.getAiTag()).isEqualTo("tag");
        assertThat(article.getSentiment()).isEqualTo("pos");
        assertThat(article.getSentimentStats()).isEqualTo("stats");
        assertThat(article.getAiRegion()).isEqualTo("region");
        assertThat(article.getAiOrg()).isEqualTo("org");
        assertThat(article.getAiSummary()).isEqualTo("summary");
        assertThat(article.getAiContent()).isEqualTo("aicontent");
        assertThat(article.isDuplicate()).isTrue();
    }
}
