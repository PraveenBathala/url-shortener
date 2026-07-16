package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class ShortCodeExpiredException extends ApplicationException {

    public ShortCodeExpiredException(String shortCode) {
        super(ErrorCode.SHORT_CODE_EXPIRED, HttpStatus.GONE, "Short code expired: " + shortCode);
    }
}
