package com.example.urlshortener.cache;

import java.io.Serializable;
import java.time.Instant;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;

public record CachedShortUrl(
        String shortCode,
        String destinationUrl,
        ShortUrlStatus status,
        Instant expiresAt) implements Serializable {

    public static CachedShortUrl from(ShortUrl shortUrl) {
        return new CachedShortUrl(
                shortUrl.getShortCode(),
                shortUrl.getDestinationUrl(),
                shortUrl.getStatus(),
                shortUrl.getExpiresAt());
    }
}
