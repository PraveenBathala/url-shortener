package com.example.urlshortener.analytics;

import java.time.Instant;

public record RedirectEvent(String shortCode, Instant occurredAt, String outcome) {
}
