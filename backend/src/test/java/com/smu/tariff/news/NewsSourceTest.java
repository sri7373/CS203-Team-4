package com.smu.tariff.news;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class NewsSourceTest {
    @Test
    void testGettersAndSetters() {
        NewsSource src = new NewsSource();
        src.setId("id");
        src.setName("name");
        src.setUrl("url");
        src.setCategory(List.of("cat"));
        src.setLanguage(List.of("en"));
        src.setCountry(List.of("us"));

        assertThat(src.getId()).isEqualTo("id");
        assertThat(src.getName()).isEqualTo("name");
        assertThat(src.getUrl()).isEqualTo("url");
        assertThat(src.getCategory()).containsExactly("cat");
        assertThat(src.getLanguage()).containsExactly("en");
        assertThat(src.getCountry()).containsExactly("us");
    }
}
