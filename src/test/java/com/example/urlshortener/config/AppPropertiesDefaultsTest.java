package com.example.urlshortener.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppPropertiesDefaultsTest {

    @Test
    void defaultConfigurationMatchesDocumentedMvpValues() {
        AppProperties properties = new AppProperties();

        assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:8080");
        assertThat(properties.getShortCode().getLength()).isEqualTo(7);
        assertThat(properties.getShortCode().getMaxGenerationAttempts()).isEqualTo(5);
        assertThat(properties.getDestinationUrl().getMaxLength()).isEqualTo(2048);
        assertThat(properties.getSecurity().isRequireApiKey()).isTrue();
        assertThat(properties.getSecurity().getRateLimit().getRequestsPerMinute()).isEqualTo(60);
        assertThat(properties.getBulk().getMaxBatchSize()).isEqualTo(20);
        assertThat(properties.getAgentic().isEnabled()).isTrue();
        assertThat(properties.getAnalytics().getPublishTimeoutMs()).isEqualTo(50);
    }
}
