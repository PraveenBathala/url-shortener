package com.example.urlshortener.api.dto;

import java.util.List;

public record BulkCreateShortUrlResponse(List<BulkCreateItemResult> results) {
}
