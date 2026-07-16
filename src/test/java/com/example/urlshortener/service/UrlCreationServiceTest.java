package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.urlshortener.api.dto.CreateShortUrlRequest;
import com.example.urlshortener.api.dto.ShortUrlResponse;
import com.example.urlshortener.api.error.InvalidRequestException;
import com.example.urlshortener.api.error.ShortCodeGenerationFailedException;
import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.generation.ShortCodeGenerator;
import com.example.urlshortener.metrics.UrlShortenerMetrics;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.example.urlshortener.validation.DestinationUrlValidator;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class UrlCreationServiceTest {

    @Mock
    private DestinationUrlValidator destinationUrlValidator;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private ShortUrlRepository shortUrlRepository;

    private UrlCreationService service;
    private final Instant now = Instant.parse("2026-07-16T20:00:00Z");

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setBaseUrl("http://localhost:8080");
        appProperties.getShortCode().setMaxGenerationAttempts(3);
        service = new UrlCreationService(
                destinationUrlValidator,
                shortCodeGenerator,
                shortUrlRepository,
                appProperties,
                new UrlShortenerMetrics(new SimpleMeterRegistry()),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void createsShortUrlOnFirstAttempt() {
        when(destinationUrlValidator.validate("https://example.com")).thenReturn("https://example.com");
        when(shortCodeGenerator.generate()).thenReturn("aB7xK9P");
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = service.create(new CreateShortUrlRequest("https://example.com", null));

        assertThat(response.shortCode()).isEqualTo("aB7xK9P");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/aB7xK9P");
        assertThat(response.status()).isEqualTo(ShortUrlStatus.ACTIVE);
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void retriesOnCollisionThenSucceeds() {
        when(destinationUrlValidator.validate("https://example.com")).thenReturn("https://example.com");
        when(shortCodeGenerator.generate()).thenReturn("collision", "success1");
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = service.create(new CreateShortUrlRequest("https://example.com", null));

        assertThat(response.shortCode()).isEqualTo("success1");
        verify(shortCodeGenerator, times(2)).generate();
    }

    @Test
    void exhaustsRetriesAndFails() {
        when(destinationUrlValidator.validate("https://example.com")).thenReturn("https://example.com");
        when(shortCodeGenerator.generate()).thenReturn("dup1", "dup2", "dup3");
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.create(new CreateShortUrlRequest("https://example.com", null)))
                .isInstanceOf(ShortCodeGenerationFailedException.class);
        verify(shortCodeGenerator, times(3)).generate();
    }

    @Test
    void rejectsPastExpiration() {
        when(destinationUrlValidator.validate("https://example.com")).thenReturn("https://example.com");

        assertThatThrownBy(() -> service.create(new CreateShortUrlRequest(
                        "https://example.com", Instant.parse("2020-01-01T00:00:00Z"))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("future");
    }
}
