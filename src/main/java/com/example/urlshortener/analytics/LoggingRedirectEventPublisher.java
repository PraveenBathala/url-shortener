package com.example.urlshortener.analytics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.metrics.UrlShortenerMetrics;

import jakarta.annotation.PreDestroy;

/**
 * In-process best-effort publisher. Events may be lost on timeout/failure.
 * Persists aggregate counters for {@code GET /api/v1/analytics/{code}}.
 * Duplicate delivery is possible if a future retry adapter is introduced later;
 * consumers should treat events as at-least-once / idempotent by shortCode+timestamp.
 */
@Component
public class LoggingRedirectEventPublisher implements RedirectEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingRedirectEventPublisher.class);

    private final long publishTimeoutMs;
    private final UrlShortenerMetrics metrics;
    private final AnalyticsPersistenceService analyticsPersistenceService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public LoggingRedirectEventPublisher(
            AppProperties appProperties,
            UrlShortenerMetrics metrics,
            AnalyticsPersistenceService analyticsPersistenceService) {
        this.publishTimeoutMs = appProperties.getAnalytics().getPublishTimeoutMs();
        this.metrics = metrics;
        this.analyticsPersistenceService = analyticsPersistenceService;
    }

    @Override
    public void publish(RedirectEvent event) {
        if (closed.get()) {
            metrics.recordAnalyticsFailure();
            return;
        }
        try {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> sink(event), executor);
            future.get(publishTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            metrics.recordAnalyticsFailure();
            log.warn(
                    "operation=analyticsPublish status=timeout shortCodeLength={}",
                    event.shortCode().length());
        } catch (Exception ex) {
            metrics.recordAnalyticsFailure();
            log.warn(
                    "operation=analyticsPublish status=failure shortCodeLength={}",
                    event.shortCode().length());
        }
    }

    private void sink(RedirectEvent event) {
        log.info(
                "operation=analyticsEvent outcome={} shortCodeLength={}",
                event.outcome(),
                event.shortCode().length());
        analyticsPersistenceService.record(event);
    }

    @PreDestroy
    void shutdown() {
        closed.set(true);
        executor.shutdownNow();
    }
}
