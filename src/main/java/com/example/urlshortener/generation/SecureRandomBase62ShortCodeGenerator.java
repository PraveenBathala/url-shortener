package com.example.urlshortener.generation;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.urlshortener.config.AppProperties;

import jakarta.annotation.PostConstruct;

@Component
public class SecureRandomBase62ShortCodeGenerator implements ShortCodeGenerator {

    public static final String BASE62_ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final SecureRandom secureRandom;
    private final int length;

    @Autowired
    public SecureRandomBase62ShortCodeGenerator(AppProperties appProperties) {
        this(appProperties.getShortCode().getLength(), new SecureRandom());
    }

    SecureRandomBase62ShortCodeGenerator(int length, SecureRandom secureRandom) {
        this.length = length;
        this.secureRandom = secureRandom;
    }

    @PostConstruct
    void validateConfiguration() {
        if (length < 4 || length > 16) {
            throw new IllegalStateException(
                    "app.short-code.length must be between 4 and 16 inclusive, but was " + length);
        }
    }

    @Override
    public String generate() {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = BASE62_ALPHABET.charAt(secureRandom.nextInt(BASE62_ALPHABET.length()));
        }
        return new String(chars);
    }
}
