package com.example.urlshortener.analytics;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "redirect_analytics")
public class RedirectAnalytics {

    @Id
    @Column(name = "short_code", nullable = false, length = 16)
    private String shortCode;

    @Column(name = "redirect_count", nullable = false)
    private long redirectCount;

    @Column(name = "not_found_count", nullable = false)
    private long notFoundCount;

    @Column(name = "expired_count", nullable = false)
    private long expiredCount;

    @Column(name = "disabled_count", nullable = false)
    private long disabledCount;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RedirectAnalytics() {
        // JPA
    }

    public RedirectAnalytics(String shortCode, Instant now) {
        this.shortCode = Objects.requireNonNull(shortCode, "shortCode");
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    public String getShortCode() {
        return shortCode;
    }

    public long getRedirectCount() {
        return redirectCount;
    }

    public long getNotFoundCount() {
        return notFoundCount;
    }

    public long getExpiredCount() {
        return expiredCount;
    }

    public long getDisabledCount() {
        return disabledCount;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void record(String outcome, Instant occurredAt) {
        switch (outcome) {
            case "REDIRECTED" -> redirectCount++;
            case "NOT_FOUND" -> notFoundCount++;
            case "EXPIRED" -> expiredCount++;
            case "DISABLED" -> disabledCount++;
            default -> {
                // ignore unknown outcomes
            }
        }
        this.lastEventAt = occurredAt;
        this.updatedAt = occurredAt;
    }
}
