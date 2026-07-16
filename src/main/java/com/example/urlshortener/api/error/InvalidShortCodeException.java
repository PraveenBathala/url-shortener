package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class InvalidShortCodeException extends ApplicationException {

    public InvalidShortCodeException(String message) {
        super(ErrorCode.INVALID_SHORT_CODE, HttpStatus.BAD_REQUEST, message);
    }
}
