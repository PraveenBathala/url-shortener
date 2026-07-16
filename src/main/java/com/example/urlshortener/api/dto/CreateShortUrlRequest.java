package com.example.urlshortener.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record CreateShortUrlRequest(
        @NotBlank(message = "destinationUrl is required") String destinationUrl,
        Instant expiresAt) {
}
