package com.smu.tariff.news;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.mockito.Mockito.*;

public class MockRestTemplateBuilder extends RestTemplateBuilder {
    private final RestTemplate restTemplate;

    public MockRestTemplateBuilder(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public RestTemplateBuilder setConnectTimeout(Duration timeout) {
        return this;
    }

    @Override
    public RestTemplateBuilder setReadTimeout(Duration timeout) {
        return this;
    }

    @Override
    public RestTemplate build() {
        return restTemplate;
    }
}
