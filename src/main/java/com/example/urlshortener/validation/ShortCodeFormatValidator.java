package com.example.urlshortener.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.example.urlshortener.api.error.InvalidShortCodeException;

@Component
public class ShortCodeFormatValidator {

    private static final Pattern BASE62 = Pattern.compile("^[0-9a-zA-Z]{4,16}$");

    public String validate(String shortCode) {
        if (shortCode == null || shortCode.isBlank() || !BASE62.matcher(shortCode).matches()) {
            throw new InvalidShortCodeException("Short code is malformed");
        }
        return shortCode;
    }
}
