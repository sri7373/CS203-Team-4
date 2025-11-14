package com.smu.tariff.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class NewsControllerTest {

    @Test
    void testGetNextPage() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsService.getNextPage("token")).thenReturn(resp);
        ResponseEntity<NewsDataResponse> result = newsController.getNextPage("token");
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(resp);
    }

    @Test
    void testGetNextPage_serviceThrows() {
        when(newsService.getNextPage(any())).thenThrow(new RuntimeException("fail"));
        assertThatThrownBy(() -> newsController.getNextPage("token")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testGetNewsSources() {
        List<NewsSource> sources = List.of(new NewsSource());
        when(newsService.getTariffNewsSources("us")).thenReturn(sources);
        ResponseEntity<List<NewsSource>> result = newsController.getNewsSources("us");
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(sources);
    }

    @Test
    void testGetNewsSources_serviceThrows() {
        when(newsService.getTariffNewsSources(any())).thenThrow(new RuntimeException("fail"));
        assertThatThrownBy(() -> newsController.getNewsSources("us")).isInstanceOf(RuntimeException.class);
    }
    @Test
    void testGetCountryTradeNews_nullCountryCode() {
        when(newsService.getCountryTradeNews(null, "cat", 5)).thenReturn(new NewsDataResponse());
        ResponseEntity<NewsDataResponse> resp = newsController.getCountryTradeNews(null, "cat", 5);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void testGetCountryTradeNews_blankCountryCode() {
        when(newsService.getCountryTradeNews("   ", "cat", 5)).thenReturn(new NewsDataResponse());
        ResponseEntity<NewsDataResponse> resp = newsController.getCountryTradeNews("   ", "cat", 5);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void testGetCountryTradeNews_nullProductCategory() {
        when(newsService.getCountryTradeNews("us", null, 5)).thenReturn(new NewsDataResponse());
        ResponseEntity<NewsDataResponse> resp = newsController.getCountryTradeNews("us", null, 5);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void testGetCountryTradeNews_emptyProductCategory() {
        when(newsService.getCountryTradeNews("us", "   ", 5)).thenReturn(new NewsDataResponse());
        ResponseEntity<NewsDataResponse> resp = newsController.getCountryTradeNews("us", "   ", 5);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void testGetCountryTradeNews_limitNull() {
        when(newsService.getCountryTradeNews("us", "cat", null)).thenReturn(new NewsDataResponse());
        ResponseEntity<NewsDataResponse> resp = newsController.getCountryTradeNews("us", "cat", null);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void testGetCountryTradeNews_limitOver50() {
        when(newsService.getCountryTradeNews("us", "cat", 100)).thenReturn(new NewsDataResponse());
        ResponseEntity<NewsDataResponse> resp = newsController.getCountryTradeNews("us", "cat", 100);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void testGetCountryTradeNews_serviceThrows() {
        when(newsService.getCountryTradeNews(any(), any(), any())).thenThrow(new RuntimeException("fail"));
        assertThatThrownBy(() -> newsController.getCountryTradeNews("us", "cat", 5)).isInstanceOf(RuntimeException.class);
    }
    NewsService newsService;
    NewsController newsController;

    @BeforeEach
    void setUp() {
        newsService = mock(NewsService.class);
        newsController = new NewsController(newsService);
    }

    @Test
    void testGetLatestTariffNews() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsService.getLatestTariffNews("us", "steel", 5)).thenReturn(resp);
        ResponseEntity<NewsDataResponse> result = newsController.getLatestTariffNews("us", "steel", 5);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(resp);
    }

    @Test
    void testSearchNews() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsService.searchNews("tariff", "us", 3)).thenReturn(resp);
        ResponseEntity<NewsDataResponse> result = newsController.searchNews("tariff", "us", 3);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(resp);
    }

    @Test
    void testGetCountryTradeNews() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsService.getCountryTradeNews("us", "agriculture", 4)).thenReturn(resp);
        ResponseEntity<NewsDataResponse> result = newsController.getCountryTradeNews("us", "agriculture", 4);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(resp);
    }

    @Test
    void testGetHistoricalTariffNews() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsService.getHistoricalTariffNews("2020-01-01", "2020-12-31", "us", 10)).thenReturn(resp);
        // NewsController method is getHistoricalNews
        ResponseEntity<NewsDataResponse> result = newsController.getHistoricalNews("2020-01-01", "2020-12-31", "us", 10);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(resp);
    }

    @Test
    void testGetTariffNewsSources() {
        List<NewsSource> sources = List.of(new NewsSource());
        when(newsService.getTariffNewsSources("us")).thenReturn(sources);
        // NewsController method is getNewsSources
        ResponseEntity<List<NewsSource>> result = newsController.getNewsSources("us");
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isSameAs(sources);
    }
}
