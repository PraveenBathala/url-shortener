package com.example.urlshortener.analytics;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.metrics.UrlShortenerMetrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class LoggingRedirectEventPublisherTest {

    @Test
    void publishCompletesWithinTimeout() {
        AppProperties properties = new AppProperties();
        properties.getAnalytics().setPublishTimeoutMs(200);
        LoggingRedirectEventPublisher publisher =
                new LoggingRedirectEventPublisher(properties, new UrlShortenerMetrics(new SimpleMeterRegistry()));

        assertThatCode(() -> publisher.publish(new RedirectEvent("aB7xK9P", Instant.parse("2026-07-16T20:00:00Z"), "REDIRECTED")))
                .doesNotThrowAnyException();

        publisher.shutdown();
    }
}
