package com.example.urlshortener.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.metrics.UrlShortenerMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class RedisShortUrlCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisShortUrlCache cache;
    private ObjectMapper objectMapper;
    private final Instant now = Instant.parse("2026-07-16T20:00:00Z");

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AppProperties properties = new AppProperties();
        properties.getCache().setDefaultTtlSeconds(3600);
        properties.getCache().setNegativeTtlSeconds(30);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cache = new RedisShortUrlCache(
                redisTemplate,
                objectMapper,
                properties,
                new UrlShortenerMetrics(new SimpleMeterRegistry()),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void getHitMissAndFailure() throws Exception {
        CachedShortUrl cached = new CachedShortUrl(
                "aB7xK9P", "https://example.com", ShortUrlStatus.ACTIVE, null);
        when(valueOperations.get("shorturl:aB7xK9P")).thenReturn(objectMapper.writeValueAsString(cached));
        assertThat(cache.get("aB7xK9P")).contains(cached);

        when(valueOperations.get("shorturl:missing")).thenReturn(null);
        assertThat(cache.get("missing")).isEmpty();

        when(valueOperations.get("shorturl:boom")).thenThrow(new RuntimeException("redis down"));
        assertThat(cache.get("boom")).isEmpty();
    }

    @Test
    void putUsesExpirationAwareTtl() {
        CachedShortUrl cached = new CachedShortUrl(
                "aB7xK9P",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                Instant.parse("2026-07-16T20:10:00Z"));

        cache.put(cached);

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("shorturl:aB7xK9P"), any(String.class), ttl.capture());
        assertThat(ttl.getValue().getSeconds()).isEqualTo(600);
    }

    @Test
    void putNegativeAndEvictTolerateFailures() {
        cache.putNegative("missing1");
        verify(valueOperations).set(eq("shorturl:missing1"), eq(RedisShortUrlCache.NEGATIVE_MARKER), any(Duration.class));

        doThrow(new RuntimeException("redis down")).when(redisTemplate).delete("shorturl:aB7xK9P");
        cache.evict("aB7xK9P");
    }
}
