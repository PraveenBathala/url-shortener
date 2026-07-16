package com.example.urlshortener.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class UrlShortenerMetrics {

    private final Counter urlsCreated;
    private final Counter redirects;
    private final Counter unknownCodes;
    private final Counter expiredCodes;
    private final Counter disabledCodes;
    private final Counter collisionRetries;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheFailures;
    private final Counter analyticsFailures;
    private final Timer creationTimer;
    private final Timer redirectTimer;

    public UrlShortenerMetrics(MeterRegistry registry) {
        this.urlsCreated = registry.counter("urlshortener.urls.created");
        this.redirects = registry.counter("urlshortener.redirects");
        this.unknownCodes = registry.counter("urlshortener.redirects.unknown");
        this.expiredCodes = registry.counter("urlshortener.redirects.expired");
        this.disabledCodes = registry.counter("urlshortener.redirects.disabled");
        this.collisionRetries = registry.counter("urlshortener.shortcode.collisions");
        this.cacheHits = registry.counter("urlshortener.cache.hits");
        this.cacheMisses = registry.counter("urlshortener.cache.misses");
        this.cacheFailures = registry.counter("urlshortener.cache.failures");
        this.analyticsFailures = registry.counter("urlshortener.analytics.publish.failures");
        this.creationTimer = registry.timer("urlshortener.urls.creation.latency");
        this.redirectTimer = registry.timer("urlshortener.redirects.latency");
    }

    public void recordCreated() {
        urlsCreated.increment();
    }

    public void recordRedirect() {
        redirects.increment();
    }

    public void recordUnknown() {
        unknownCodes.increment();
    }

    public void recordExpired() {
        expiredCodes.increment();
    }

    public void recordDisabled() {
        disabledCodes.increment();
    }

    public void recordCollisionRetry() {
        collisionRetries.increment();
    }

    public void recordCacheHit() {
        cacheHits.increment();
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    public void recordCacheFailure() {
        cacheFailures.increment();
    }

    public void recordAnalyticsFailure() {
        analyticsFailures.increment();
    }

    public void recordCreationLatency(long nanos) {
        creationTimer.record(nanos, TimeUnit.NANOSECONDS);
    }

    public void recordRedirectLatency(long nanos) {
        redirectTimer.record(nanos, TimeUnit.NANOSECONDS);
    }
}
