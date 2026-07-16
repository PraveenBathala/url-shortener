package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.urlshortener.analytics.RedirectEventPublisher;
import com.example.urlshortener.api.error.InvalidShortCodeException;
import com.example.urlshortener.api.error.ShortCodeDisabledException;
import com.example.urlshortener.api.error.ShortCodeExpiredException;
import com.example.urlshortener.api.error.ShortCodeNotFoundException;
import com.example.urlshortener.cache.CachedShortUrl;
import com.example.urlshortener.cache.ShortUrlCache;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.metrics.UrlShortenerMetrics;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.example.urlshortener.validation.ShortCodeFormatValidator;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class RedirectServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortUrlCache shortUrlCache;

    @Mock
    private RedirectEventPublisher redirectEventPublisher;

    private RedirectService service;
    private final Instant now = Instant.parse("2026-07-16T20:00:00Z");

    @BeforeEach
    void setUp() {
        service = new RedirectService(
                new ShortCodeFormatValidator(),
                shortUrlRepository,
                shortUrlCache,
                redirectEventPublisher,
                new UrlShortenerMetrics(new SimpleMeterRegistry()),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void resolvesActiveUnexpiredUrlAndCaches() {
        when(shortUrlCache.get("aB7xK9P")).thenReturn(Optional.empty());
        when(shortUrlRepository.findById("aB7xK9P")).thenReturn(Optional.of(active("aB7xK9P", null)));

        assertThat(service.resolveDestination("aB7xK9P")).isEqualTo("https://example.com/products/123");
        verify(shortUrlCache).put(any(CachedShortUrl.class));
        verify(redirectEventPublisher).publish(any());
    }

    @Test
    void usesCacheHitWithoutDatabase() {
        when(shortUrlCache.get("aB7xK9P"))
                .thenReturn(Optional.of(new CachedShortUrl(
                        "aB7xK9P",
                        "https://example.com/products/123",
                        ShortUrlStatus.ACTIVE,
                        null)));

        assertThat(service.resolveDestination("aB7xK9P")).isEqualTo("https://example.com/products/123");
        verify(shortUrlRepository, never()).findById(any());
    }

    @Test
    void fallsBackToDatabaseWhenCacheEmpty() {
        when(shortUrlCache.get("aB7xK9P")).thenReturn(Optional.empty());
        when(shortUrlRepository.findById("aB7xK9P")).thenReturn(Optional.of(active("aB7xK9P", null)));

        assertThat(service.resolveDestination("aB7xK9P")).isEqualTo("https://example.com/products/123");
    }

    @Test
    void rejectsUnknownExpiredDisabledAndMalformed() {
        when(shortUrlCache.get("missing1")).thenReturn(Optional.empty());
        when(shortUrlRepository.findById("missing1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolveDestination("missing1"))
                .isInstanceOf(ShortCodeNotFoundException.class);
        verify(shortUrlCache).putNegative("missing1");

        when(shortUrlCache.get("expired1")).thenReturn(Optional.empty());
        when(shortUrlRepository.findById("expired1"))
                .thenReturn(Optional.of(active("expired1", Instant.parse("2026-01-01T00:00:00Z"))));
        assertThatThrownBy(() -> service.resolveDestination("expired1"))
                .isInstanceOf(ShortCodeExpiredException.class);

        ShortUrl disabled = active("disable1", null);
        disabled.disable(now);
        when(shortUrlCache.get("disable1")).thenReturn(Optional.empty());
        when(shortUrlRepository.findById("disable1")).thenReturn(Optional.of(disabled));
        assertThatThrownBy(() -> service.resolveDestination("disable1"))
                .isInstanceOf(ShortCodeDisabledException.class);

        assertThatThrownBy(() -> service.resolveDestination("bad!"))
                .isInstanceOf(InvalidShortCodeException.class);
    }

    @Test
    void continuesWhenAnalyticsPublisherThrows() {
        when(shortUrlCache.get("aB7xK9P")).thenReturn(Optional.empty());
        when(shortUrlRepository.findById("aB7xK9P")).thenReturn(Optional.of(active("aB7xK9P", null)));
        org.mockito.Mockito.doThrow(new RuntimeException("broker down"))
                .when(redirectEventPublisher)
                .publish(any());

        assertThat(service.resolveDestination("aB7xK9P")).isEqualTo("https://example.com/products/123");
    }

    private ShortUrl active(String code, Instant expiresAt) {
        return new ShortUrl(
                code,
                "https://example.com/products/123",
                ShortUrlStatus.ACTIVE,
                now,
                now,
                expiresAt);
    }
}
