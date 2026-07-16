package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class ShortCodeDisabledException extends ApplicationException {

    public ShortCodeDisabledException(String shortCode) {
        super(ErrorCode.SHORT_CODE_DISABLED, HttpStatus.GONE, "Short code disabled: " + shortCode);
    }
}
