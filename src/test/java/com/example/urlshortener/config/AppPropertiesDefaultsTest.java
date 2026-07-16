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
    }
}
