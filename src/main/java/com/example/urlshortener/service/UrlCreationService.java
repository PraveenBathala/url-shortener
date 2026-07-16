package com.example.urlshortener.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class UrlCreationService {

    private static final Logger log = LoggerFactory.getLogger(UrlCreationService.class);

    private final DestinationUrlValidator destinationUrlValidator;
    private final ShortCodeGenerator shortCodeGenerator;
    private final ShortUrlRepository shortUrlRepository;
    private final AppProperties appProperties;
    private final UrlShortenerMetrics metrics;
    private final Clock clock;

    public UrlCreationService(
            DestinationUrlValidator destinationUrlValidator,
            ShortCodeGenerator shortCodeGenerator,
            ShortUrlRepository shortUrlRepository,
            AppProperties appProperties,
            UrlShortenerMetrics metrics,
            Clock clock) {
        this.destinationUrlValidator = destinationUrlValidator;
        this.shortCodeGenerator = shortCodeGenerator;
        this.shortUrlRepository = shortUrlRepository;
        this.appProperties = appProperties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ShortUrlResponse create(CreateShortUrlRequest request) {
        long started = System.nanoTime();
        try {
            String destinationUrl = destinationUrlValidator.validate(request.destinationUrl());
            Instant now = clock.instant();
            Instant expiresAt = validateExpiration(request.expiresAt(), now);

            int maxAttempts = appProperties.getShortCode().getMaxGenerationAttempts();
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                String shortCode = shortCodeGenerator.generate();
                ShortUrl entity = new ShortUrl(
                        shortCode,
                        destinationUrl,
                        ShortUrlStatus.ACTIVE,
                        now,
                        now,
                        expiresAt);
                try {
                    ShortUrl saved = shortUrlRepository.saveAndFlush(entity);
                    metrics.recordCreated();
                    log.info(
                            "operation=createUrl status=success shortCodeLength={} attempt={}",
                            saved.getShortCode().length(),
                            attempt);
                    return ShortUrlResponse.from(saved, appProperties.getBaseUrl());
                } catch (DataIntegrityViolationException ex) {
                    metrics.recordCollisionRetry();
                    log.info(
                            "operation=createUrl status=collision attempt={} maxAttempts={}",
                            attempt,
                            maxAttempts);
                    if (attempt == maxAttempts) {
                        throw new ShortCodeGenerationFailedException(maxAttempts);
                    }
                }
            }

            throw new ShortCodeGenerationFailedException(maxAttempts);
        } finally {
            metrics.recordCreationLatency(System.nanoTime() - started);
        }
    }

    private Instant validateExpiration(Instant expiresAt, Instant now) {
        if (expiresAt == null) {
            return null;
        }
        if (!expiresAt.isAfter(now)) {
            throw new InvalidRequestException("expiresAt must be in the future");
        }
        return expiresAt;
    }
}
