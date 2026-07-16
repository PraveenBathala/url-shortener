package com.example.urlshortener.api.error;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String requestId) {
}
