package com.example.urlshortener.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.urlshortener.analytics.RedirectAnalytics;
import com.example.urlshortener.analytics.RedirectAnalyticsRepository;
import com.example.urlshortener.api.dto.AnalyticsResponse;
import com.example.urlshortener.api.error.ShortCodeNotFoundException;
import com.example.urlshortener.persistence.ShortUrlRepository;
import com.example.urlshortener.validation.ShortCodeFormatValidator;

@Service
public class AnalyticsQueryService {

    private final ShortCodeFormatValidator shortCodeFormatValidator;
    private final ShortUrlRepository shortUrlRepository;
    private final RedirectAnalyticsRepository analyticsRepository;

    public AnalyticsQueryService(
            ShortCodeFormatValidator shortCodeFormatValidator,
            ShortUrlRepository shortUrlRepository,
            RedirectAnalyticsRepository analyticsRepository) {
        this.shortCodeFormatValidator = shortCodeFormatValidator;
        this.shortUrlRepository = shortUrlRepository;
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String rawShortCode) {
        String shortCode = shortCodeFormatValidator.validate(rawShortCode);
        if (!shortUrlRepository.existsById(shortCode)) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        RedirectAnalytics analytics = analyticsRepository
                .findById(shortCode)
                .orElse(null);
        if (analytics == null) {
            return AnalyticsResponse.empty(shortCode);
        }
        return AnalyticsResponse.from(analytics);
    }
}
