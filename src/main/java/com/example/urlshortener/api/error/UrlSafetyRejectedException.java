package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class UrlSafetyRejectedException extends ApplicationException {

    public UrlSafetyRejectedException(String message) {
        super(ErrorCode.URL_SAFETY_REJECTED, HttpStatus.BAD_REQUEST, message);
    }
}
