package com.smu.tariff.news;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NewsServiceTest {
    @Test
    void testGetLatestTariffNews_queryLengthOver100() {
        // Create a productCategory that will make the query string > 100 chars
        String longCategory = "verylongcategorynameverylongcategorynameverylongcategorynameverylongcategorynameverylongcategorynameverylongcategoryname";
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getLatestTariffNews("us", longCategory, 5);
        assertThat(result).isSameAs(resp);
        // Optionally verify that the query in the request is the default (cannot directly check without argument captor)
        verify(newsDataClient).getLatestNews(any());
    }

    @Test
    void testGetNextPage_validToken() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getNextPage("sometoken");
        assertThat(result).isSameAs(resp);
    }

    @Test
    void testGetNextPage_nullOrBlankTokenThrows() {
        assertThatThrownBy(() -> newsService.getNextPage(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newsService.getNextPage("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newsService.getNextPage("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetTariffNewsSources_withCountry() {
        List<NewsSource> sources = List.of(new NewsSource());
        when(newsDataClient.getSources(any())).thenReturn(sources);
        List<NewsSource> result = newsService.getTariffNewsSources("us");
        assertThat(result).isSameAs(sources);
    }

    @Test
    void testGetTariffNewsSources_nullCountry() {
        List<NewsSource> sources = List.of(new NewsSource());
        when(newsDataClient.getSources(any())).thenReturn(sources);
        List<NewsSource> result = newsService.getTariffNewsSources(null);
        assertThat(result).isSameAs(sources);
    }

    @Test
    void testGetTariffNewsSources_emptyCountry() {
        List<NewsSource> sources = List.of(new NewsSource());
        when(newsDataClient.getSources(any())).thenReturn(sources);
        List<NewsSource> result = newsService.getTariffNewsSources("");
        assertThat(result).isSameAs(sources);
    }
    @Test
    void testGetCountryTradeNews_nullCountryCode() {
        NewsDataResponse resp = newsService.getCountryTradeNews(null, "cat", 5);
        assertThat(resp.getTotalResults()).isEqualTo(0);
        assertThat(resp.getArticles()).isEmpty();
    }

    @Test
    void testGetCountryTradeNews_blankCountryCode() {
        NewsDataResponse resp = newsService.getCountryTradeNews("   ", "cat", 5);
        assertThat(resp.getTotalResults()).isEqualTo(0);
        assertThat(resp.getArticles()).isEmpty();
    }

    @Test
    void testGetCountryTradeNews_nullProductCategory() {
        NewsDataResponse mockResp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(mockResp);
        NewsDataResponse resp = newsService.getCountryTradeNews("us", null, 5);
        assertThat(resp).isSameAs(mockResp);
        verify(newsDataClient).getLatestNews(any());
    }

    @Test
    void testGetCountryTradeNews_emptyProductCategory() {
        NewsDataResponse mockResp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(mockResp);
        NewsDataResponse resp = newsService.getCountryTradeNews("us", "   ", 5);
        assertThat(resp).isSameAs(mockResp);
        verify(newsDataClient).getLatestNews(any());
    }

    @Test
    void testGetCountryTradeNews_limitNull() {
        NewsDataResponse mockResp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(mockResp);
        NewsDataResponse resp = newsService.getCountryTradeNews("us", "cat", null);
        assertThat(resp).isSameAs(mockResp);
        verify(newsDataClient).getLatestNews(any());
    }

    @Test
    void testGetCountryTradeNews_limitOver50() {
        NewsDataResponse mockResp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(mockResp);
        NewsDataResponse resp = newsService.getCountryTradeNews("us", "cat", 100);
        assertThat(resp).isSameAs(mockResp);
        verify(newsDataClient).getLatestNews(any());
    }

    @Test
    void testGetCountryTradeNews_serviceThrows() {
        when(newsDataClient.getLatestNews(any())).thenThrow(new RuntimeException("fail"));
        assertThatThrownBy(() -> newsService.getCountryTradeNews("us", "cat", 5)).isInstanceOf(RuntimeException.class);
    }
    @Test
    void testGetLatestTariffNews_nullsAndZeroLimit() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        assertThat(newsService.getLatestTariffNews(null, null, 0)).isSameAs(resp);
    }

    @Test
    void testGetLatestTariffNews_newsDataClientThrows() {
        when(newsDataClient.getLatestNews(any())).thenThrow(new RuntimeException("fail"));
        assertThatThrownBy(() -> newsService.getLatestTariffNews("us", "steel", 5)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testSearchNews_nullQuery() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        assertThat(newsService.searchNews(null, null, 1)).isSameAs(resp);
    }

    @Test
    void testGetCountryTradeNews_nulls() {
        NewsDataResponse result = newsService.getCountryTradeNews(null, null, 1);
        assertThat(result.getTotalResults()).isEqualTo(0);
        assertThat(result.getArticles()).isEmpty();
    }

    @Test
    void testGetHistoricalTariffNews_newsDataClientThrows() {
        when(newsDataClient.getArchiveNews(any())).thenThrow(new RuntimeException("fail"));
        assertThatThrownBy(() -> newsService.getHistoricalTariffNews("2020-01-01", "2020-12-31", "us", 10)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testGetNextPage_emptyResult() {
        when(newsDataClient.getLatestNews(any())).thenReturn(null);
        assertThat(newsService.getNextPage("token")).isNull();
    }

    @Test
    void testGetTariffNewsSources_emptyList() {
        when(newsDataClient.getSources(any())).thenReturn(java.util.Collections.emptyList());
        assertThat(newsService.getTariffNewsSources("us")).isEmpty();
    }
    @Mock NewsDataClient newsDataClient;
    NewsService newsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        newsService = new NewsService(newsDataClient);
    }

    @Test
    void testGetLatestTariffNews_withCategoryAndLimit() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getLatestTariffNews("us", "steel", 5);
        assertThat(result).isSameAs(resp);
        verify(newsDataClient).getLatestNews(any());
    }

    @Test
    void testGetLatestTariffNews_overloaded() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getLatestTariffNews("us", 3);
        assertThat(result).isSameAs(resp);
    }

    @Test
    void testSearchNews() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.searchNews("tariff", "us", 2);
        assertThat(result).isSameAs(resp);
    }

    @Test
    void testGetCountryTradeNews_withCategory() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getCountryTradeNews("us", "agriculture", 4);
        assertThat(result).isSameAs(resp);
    }

    @Test
    void testGetCountryTradeNews_overloaded() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getCountryTradeNews("us", 2);
        assertThat(result).isSameAs(resp);
    }

    @Test
    void testGetHistoricalTariffNews() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getArchiveNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getHistoricalTariffNews("2020-01-01", "2020-12-31", "us", 10);
        assertThat(result).isSameAs(resp);
    }

    @Test
    void testGetNextPage_withToken() {
        NewsDataResponse resp = new NewsDataResponse();
        when(newsDataClient.getLatestNews(any())).thenReturn(resp);
        NewsDataResponse result = newsService.getNextPage("token123");
        assertThat(result).isSameAs(resp);
    }

    @Test
    void testGetNextPage_throwsOnNullToken() {
        assertThatThrownBy(() -> newsService.getNextPage(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newsService.getNextPage("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetTariffNewsSources() {
        List<NewsSource> sources = List.of(new NewsSource());
        when(newsDataClient.getSources(any())).thenReturn(sources);
        List<NewsSource> result = newsService.getTariffNewsSources("us");
        assertThat(result).isSameAs(sources);
    }
}
