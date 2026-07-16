package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.example.urlshortener.api.error.ShortCodeNotFoundException;
import com.example.urlshortener.cache.ShortUrlCache;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.example.urlshortener.validation.ShortCodeFormatValidator;

@ExtendWith(MockitoExtension.class)
class UrlManagementServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private ShortUrlCache shortUrlCache;

    private UrlManagementService service;
    private final Instant now = Instant.parse("2026-07-16T20:00:00Z");

    @BeforeEach
    void setUp() {
        service = new UrlManagementService(
                new ShortCodeFormatValidator(),
                shortUrlRepository,
                shortUrlCache,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void disablesAndEvictsCache() {
        ShortUrl entity = new ShortUrl(
                "aB7xK9P",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                now,
                now,
                null);
        when(shortUrlRepository.findById("aB7xK9P")).thenReturn(Optional.of(entity));
        when(shortUrlRepository.saveAndFlush(entity)).thenReturn(entity);

        service.disable("aB7xK9P");

        assertThat(entity.isDisabled()).isTrue();
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        verify(shortUrlCache).evict("aB7xK9P");
    }

    @Test
    void missingCodeReturnsNotFound() {
        when(shortUrlRepository.findById("missing1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.disable("missing1"))
                .isInstanceOf(ShortCodeNotFoundException.class);
    }
}
