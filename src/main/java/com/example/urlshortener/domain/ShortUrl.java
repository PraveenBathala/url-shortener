package com.example.urlshortener.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

    @Id
    @Column(name = "short_code", nullable = false, length = 16)
    private String shortCode;

    @Column(name = "destination_url", nullable = false, columnDefinition = "TEXT")
    private String destinationUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShortUrlStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ShortUrl() {
        // JPA
    }

    public ShortUrl(
            String shortCode,
            String destinationUrl,
            ShortUrlStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt) {
        this.shortCode = Objects.requireNonNull(shortCode, "shortCode");
        this.destinationUrl = Objects.requireNonNull(destinationUrl, "destinationUrl");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.expiresAt = expiresAt;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getDestinationUrl() {
        return destinationUrl;
    }

    public ShortUrlStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getVersion() {
        return version;
    }

    public boolean isDisabled() {
        return status == ShortUrlStatus.DISABLED;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public void disable(Instant updatedAt) {
        this.status = ShortUrlStatus.DISABLED;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
