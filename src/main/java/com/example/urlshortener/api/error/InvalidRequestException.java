package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApplicationException {

    public InvalidRequestException(String message) {
        super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, message);
    }
}
