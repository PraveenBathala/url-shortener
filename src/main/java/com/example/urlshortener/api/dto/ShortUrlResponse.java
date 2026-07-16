package com.example.urlshortener.api.dto;

import java.time.Instant;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;

public record ShortUrlResponse(
        String shortCode,
        String shortUrl,
        String destinationUrl,
        Instant createdAt,
        Instant expiresAt,
        ShortUrlStatus status) {

    public static ShortUrlResponse from(ShortUrl entity, String baseUrl) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return new ShortUrlResponse(
                entity.getShortCode(),
                normalizedBase + "/" + entity.getShortCode(),
                entity.getDestinationUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getStatus());
    }
}
