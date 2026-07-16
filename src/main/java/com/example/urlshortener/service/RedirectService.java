package com.example.urlshortener.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.urlshortener.analytics.RedirectEvent;
import com.example.urlshortener.analytics.RedirectEventPublisher;
import com.example.urlshortener.api.error.ShortCodeDisabledException;
import com.example.urlshortener.api.error.ShortCodeExpiredException;
import com.example.urlshortener.api.error.ShortCodeNotFoundException;
import com.example.urlshortener.cache.CachedShortUrl;
import com.example.urlshortener.cache.RedisShortUrlCache;
import com.example.urlshortener.cache.ShortUrlCache;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;
import com.example.urlshortener.metrics.UrlShortenerMetrics;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.example.urlshortener.validation.ShortCodeFormatValidator;

@Service
public class RedirectService {

    private static final Logger log = LoggerFactory.getLogger(RedirectService.class);

    private final ShortCodeFormatValidator shortCodeFormatValidator;
    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlCache shortUrlCache;
    private final RedirectEventPublisher redirectEventPublisher;
    private final UrlShortenerMetrics metrics;
    private final Clock clock;

    public RedirectService(
            ShortCodeFormatValidator shortCodeFormatValidator,
            ShortUrlRepository shortUrlRepository,
            ShortUrlCache shortUrlCache,
            RedirectEventPublisher redirectEventPublisher,
            UrlShortenerMetrics metrics,
            Clock clock) {
        this.shortCodeFormatValidator = shortCodeFormatValidator;
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlCache = shortUrlCache;
        this.redirectEventPublisher = redirectEventPublisher;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public String resolveDestination(String rawShortCode) {
        long started = System.nanoTime();
        try {
            String shortCode = shortCodeFormatValidator.validate(rawShortCode);
            Instant now = clock.instant();

            CachedShortUrl cached = shortUrlCache.get(shortCode).orElse(null);
            if (cached != null) {
                if (RedisShortUrlCache.NEGATIVE_MARKER.equals(cached.destinationUrl())) {
                    metrics.recordUnknown();
                    publish(shortCode, "NOT_FOUND");
                    throw new ShortCodeNotFoundException(shortCode);
                }
                return finalizeCached(cached, now);
            }

            ShortUrl shortUrl = shortUrlRepository.findById(shortCode).orElse(null);
            if (shortUrl == null) {
                shortUrlCache.putNegative(shortCode);
                metrics.recordUnknown();
                publish(shortCode, "NOT_FOUND");
                throw new ShortCodeNotFoundException(shortCode);
            }

            if (shortUrl.isDisabled()) {
                metrics.recordDisabled();
                publish(shortCode, "DISABLED");
                throw new ShortCodeDisabledException(shortCode);
            }
            if (shortUrl.isExpired(now)) {
                metrics.recordExpired();
                publish(shortCode, "EXPIRED");
                throw new ShortCodeExpiredException(shortCode);
            }

            shortUrlCache.put(CachedShortUrl.from(shortUrl));
            metrics.recordRedirect();
            publish(shortCode, "REDIRECTED");
            log.info("operation=redirect status=success cacheResult=miss shortCodeLength={}", shortCode.length());
            return shortUrl.getDestinationUrl();
        } finally {
            metrics.recordRedirectLatency(System.nanoTime() - started);
        }
    }

    private String finalizeCached(CachedShortUrl cached, Instant now) {
        String shortCode = cached.shortCode();
        if (cached.status() == ShortUrlStatus.DISABLED) {
            metrics.recordDisabled();
            publish(shortCode, "DISABLED");
            throw new ShortCodeDisabledException(shortCode);
        }
        if (cached.expiresAt() != null && !cached.expiresAt().isAfter(now)) {
            shortUrlCache.evict(shortCode);
            metrics.recordExpired();
            publish(shortCode, "EXPIRED");
            throw new ShortCodeExpiredException(shortCode);
        }
        metrics.recordRedirect();
        publish(shortCode, "REDIRECTED");
        log.info("operation=redirect status=success cacheResult=hit shortCodeLength={}", shortCode.length());
        return cached.destinationUrl();
    }

    private void publish(String shortCode, String outcome) {
        try {
            redirectEventPublisher.publish(new RedirectEvent(shortCode, clock.instant(), outcome));
        } catch (RuntimeException ex) {
            metrics.recordAnalyticsFailure();
            log.warn("operation=analyticsPublish status=unexpectedFailure shortCodeLength={}", shortCode.length());
        }
    }
}
