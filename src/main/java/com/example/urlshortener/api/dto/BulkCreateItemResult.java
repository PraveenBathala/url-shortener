package com.example.urlshortener.api.dto;

public record BulkCreateItemResult(
        int index, String status, ShortUrlResponse shortUrl, String errorCode, String message) {

    public static BulkCreateItemResult created(int index, ShortUrlResponse shortUrl) {
        return new BulkCreateItemResult(index, "CREATED", shortUrl, null, null);
    }

    public static BulkCreateItemResult failed(int index, String errorCode, String message) {
        return new BulkCreateItemResult(index, "FAILED", null, errorCode, message);
    }
}
