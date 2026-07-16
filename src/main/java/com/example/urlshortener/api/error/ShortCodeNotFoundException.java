package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class ShortCodeNotFoundException extends ApplicationException {

    public ShortCodeNotFoundException(String shortCode) {
        super(ErrorCode.SHORT_CODE_NOT_FOUND, HttpStatus.NOT_FOUND, "Short code not found: " + shortCode);
    }
}
