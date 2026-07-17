package com.example.urlshortener.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BulkCreateShortUrlRequest(
        @NotEmpty(message = "urls must not be empty") @Valid List<CreateShortUrlRequest> urls) {
}
