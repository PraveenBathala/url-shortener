package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class InvalidDestinationUrlException extends ApplicationException {

    public InvalidDestinationUrlException(String message) {
        super(ErrorCode.INVALID_DESTINATION_URL, HttpStatus.BAD_REQUEST, message);
    }
}
