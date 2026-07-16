package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class ShortCodeGenerationFailedException extends ApplicationException {

    public ShortCodeGenerationFailedException(int attempts) {
        super(
                ErrorCode.SHORT_CODE_GENERATION_FAILED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to generate a unique short code after " + attempts + " attempts");
    }
}
