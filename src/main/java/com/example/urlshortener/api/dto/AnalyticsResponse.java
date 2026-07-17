package com.example.urlshortener.api.dto;

import java.time.Instant;

import com.example.urlshortener.analytics.RedirectAnalytics;

public record AnalyticsResponse(
        String shortCode,
        long redirectCount,
        long notFoundCount,
        long expiredCount,
        long disabledCount,
        Instant lastEventAt) {

    public static AnalyticsResponse from(RedirectAnalytics analytics) {
        return new AnalyticsResponse(
                analytics.getShortCode(),
                analytics.getRedirectCount(),
                analytics.getNotFoundCount(),
                analytics.getExpiredCount(),
                analytics.getDisabledCount(),
                analytics.getLastEventAt());
    }

    public static AnalyticsResponse empty(String shortCode) {
        return new AnalyticsResponse(shortCode, 0, 0, 0, 0, null);
    }
}
