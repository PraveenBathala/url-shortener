package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.urlshortener.agentic.RiskLevel;
import com.example.urlshortener.agentic.UrlSafetyAgent;
import com.example.urlshortener.agentic.UrlSafetyAssessment;
import com.example.urlshortener.analytics.RedirectEventPublisher;
import com.example.urlshortener.api.dto.CreateShortUrlRequest;
import com.example.urlshortener.api.error.ShortCodeGenerationFailedException;
import com.example.urlshortener.audit.AuditLogger;
import com.example.urlshortener.cache.ShortUrlCache;
import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.generation.ShortCodeGenerator;
import com.example.urlshortener.metrics.UrlShortenerMetrics;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.example.urlshortener.validation.DestinationUrlValidator;
import com.example.urlshortener.validation.ShortCodeFormatValidator;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class ReliabilityBehaviorTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortUrlCache shortUrlCache;

    @Mock
    private RedirectEventPublisher redirectEventPublisher;

    @Mock
    private DestinationUrlValidator destinationUrlValidator;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private UrlSafetyAgent urlSafetyAgent;

    @Mock
    private AuditLogger auditLogger;

    private final Instant now = Instant.parse("2026-07-16T20:00:00Z");

    @Test
    void redisFailureFallsBackToPostgres() {
        when(shortUrlCache.get("aB7xK9P")).thenReturn(Optional.empty()); // cache miss/failure path
        when(shortUrlRepository.findById("aB7xK9P")).thenReturn(Optional.of(new ShortUrl(
                "aB7xK9P",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                now,
                now,
                null)));

        RedirectService redirectService = new RedirectService(
                new ShortCodeFormatValidator(),
                shortUrlRepository,
                shortUrlCache,
                redirectEventPublisher,
                new UrlShortenerMetrics(new SimpleMeterRegistry()),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(redirectService.resolveDestination("aB7xK9P")).isEqualTo("https://example.com");
    }

    @Test
    void analyticsFailureDoesNotBreakRedirect() {
        when(shortUrlCache.get("aB7xK9P")).thenReturn(Optional.empty());
        when(shortUrlRepository.findById("aB7xK9P")).thenReturn(Optional.of(new ShortUrl(
                "aB7xK9P",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                now,
                now,
                null)));
        org.mockito.Mockito.doThrow(new RuntimeException("timeout"))
                .when(redirectEventPublisher)
                .publish(any());

        RedirectService redirectService = new RedirectService(
                new ShortCodeFormatValidator(),
                shortUrlRepository,
                shortUrlCache,
                redirectEventPublisher,
                new UrlShortenerMetrics(new SimpleMeterRegistry()),
                Clock.fixed(now, ZoneOffset.UTC));

        assertThat(redirectService.resolveDestination("aB7xK9P")).isEqualTo("https://example.com");
    }

    @Test
    void collisionRetriesAreBounded() {
        AppProperties properties = new AppProperties();
        properties.getShortCode().setMaxGenerationAttempts(2);
        AtomicInteger attempts = new AtomicInteger();
        when(destinationUrlValidator.validate("https://example.com")).thenReturn("https://example.com");
        when(urlSafetyAgent.assess(anyString()))
                .thenReturn(new UrlSafetyAssessment(RiskLevel.LOW, "ALLOW", List.of(), List.of()));
        when(shortCodeGenerator.generate()).thenAnswer(invocation -> "code" + attempts.incrementAndGet());
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        UrlCreationService creationService = new UrlCreationService(
                destinationUrlValidator,
                shortCodeGenerator,
                shortUrlRepository,
                properties,
                new UrlShortenerMetrics(new SimpleMeterRegistry()),
                Clock.fixed(now, ZoneOffset.UTC),
                urlSafetyAgent,
                auditLogger);

        assertThatThrownBy(() -> creationService.create(new CreateShortUrlRequest("https://example.com", null)))
                .isInstanceOf(ShortCodeGenerationFailedException.class);
        assertThat(attempts.get()).isEqualTo(2);
    }
}
