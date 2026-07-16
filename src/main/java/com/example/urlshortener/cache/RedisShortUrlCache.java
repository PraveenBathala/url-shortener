package com.example.urlshortener.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.metrics.UrlShortenerMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RedisShortUrlCache implements ShortUrlCache {

    private static final Logger log = LoggerFactory.getLogger(RedisShortUrlCache.class);
    public static final String KEY_PREFIX = "shorturl:";
    public static final String NEGATIVE_MARKER = "__NEGATIVE__";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final UrlShortenerMetrics metrics;
    private final Clock clock;

    public RedisShortUrlCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            UrlShortenerMetrics metrics,
            Clock clock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    public Optional<CachedShortUrl> get(String shortCode) {
        try {
            String value = redisTemplate.opsForValue().get(key(shortCode));
            if (value == null) {
                metrics.recordCacheMiss();
                return Optional.empty();
            }
            if (NEGATIVE_MARKER.equals(value)) {
                metrics.recordCacheHit();
                return Optional.of(new CachedShortUrl(shortCode, NEGATIVE_MARKER, null, null));
            }
            CachedShortUrl cached = objectMapper.readValue(value, CachedShortUrl.class);
            metrics.recordCacheHit();
            return Optional.of(cached);
        } catch (RuntimeException | JsonProcessingException ex) {
            metrics.recordCacheFailure();
            log.warn("operation=cacheGet status=failure shortCodeLength={}", shortCode.length());
            return Optional.empty();
        }
    }

    @Override
    public void put(CachedShortUrl cachedShortUrl) {
        try {
            Duration ttl = resolveTtl(cachedShortUrl.expiresAt());
            if (ttl.isZero() || ttl.isNegative()) {
                return;
            }
            String payload = objectMapper.writeValueAsString(cachedShortUrl);
            redisTemplate.opsForValue().set(key(cachedShortUrl.shortCode()), payload, ttl);
        } catch (RuntimeException | JsonProcessingException ex) {
            metrics.recordCacheFailure();
            log.warn(
                    "operation=cachePut status=failure shortCodeLength={}",
                    cachedShortUrl.shortCode().length());
        }
    }

    @Override
    public void putNegative(String shortCode) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(
                            key(shortCode),
                            NEGATIVE_MARKER,
                            Duration.ofSeconds(appProperties.getCache().getNegativeTtlSeconds()));
        } catch (RuntimeException ex) {
            metrics.recordCacheFailure();
            log.warn("operation=cachePutNegative status=failure shortCodeLength={}", shortCode.length());
        }
    }

    @Override
    public void evict(String shortCode) {
        try {
            redisTemplate.delete(key(shortCode));
        } catch (RuntimeException ex) {
            metrics.recordCacheFailure();
            log.warn("operation=cacheEvict status=failure shortCodeLength={}", shortCode.length());
        }
    }

    private Duration resolveTtl(Instant expiresAt) {
        long defaultTtl = appProperties.getCache().getDefaultTtlSeconds();
        if (expiresAt == null) {
            return Duration.ofSeconds(defaultTtl);
        }
        long secondsUntilExpiry = Duration.between(clock.instant(), expiresAt).getSeconds();
        if (secondsUntilExpiry <= 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(Math.min(defaultTtl, secondsUntilExpiry));
    }

    private static String key(String shortCode) {
        return KEY_PREFIX + shortCode;
    }
}
