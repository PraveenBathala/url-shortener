package com.example.urlshortener.service;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.urlshortener.api.error.ShortCodeNotFoundException;
import com.example.urlshortener.cache.ShortUrlCache;
import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.example.urlshortener.validation.ShortCodeFormatValidator;

@Service
public class UrlManagementService {

    private static final Logger log = LoggerFactory.getLogger(UrlManagementService.class);

    private final ShortCodeFormatValidator shortCodeFormatValidator;
    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlCache shortUrlCache;
    private final Clock clock;

    public UrlManagementService(
            ShortCodeFormatValidator shortCodeFormatValidator,
            ShortUrlRepository shortUrlRepository,
            ShortUrlCache shortUrlCache,
            Clock clock) {
        this.shortCodeFormatValidator = shortCodeFormatValidator;
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlCache = shortUrlCache;
        this.clock = clock;
    }

    /**
     * Soft-disables a short URL. Authentication is required before public production exposure.
     */
    @Transactional
    public void disable(String rawShortCode) {
        String shortCode = shortCodeFormatValidator.validate(rawShortCode);
        ShortUrl shortUrl = shortUrlRepository
                .findById(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
        shortUrl.disable(clock.instant());
        shortUrlRepository.saveAndFlush(shortUrl);
        shortUrlCache.evict(shortCode);
        log.info("operation=disableUrl status=success shortCodeLength={}", shortCode.length());
    }
}
