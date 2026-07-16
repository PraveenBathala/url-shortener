package com.example.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    @NotBlank
    private String baseUrl = "http://localhost:8080";

    private final ShortCode shortCode = new ShortCode();
    private final DestinationUrl destinationUrl = new DestinationUrl();
    private final Cache cache = new Cache();
    private final Analytics analytics = new Analytics();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ShortCode getShortCode() {
        return shortCode;
    }

    public DestinationUrl getDestinationUrl() {
        return destinationUrl;
    }

    public Cache getCache() {
        return cache;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public static class ShortCode {

        @Min(4)
        @Max(16)
        private int length = 7;

        @Positive
        private int maxGenerationAttempts = 5;

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getMaxGenerationAttempts() {
            return maxGenerationAttempts;
        }

        public void setMaxGenerationAttempts(int maxGenerationAttempts) {
            this.maxGenerationAttempts = maxGenerationAttempts;
        }
    }

    public static class DestinationUrl {

        @Positive
        private int maxLength = 2048;

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }

    public static class Cache {

        @Positive
        private long defaultTtlSeconds = 3600;

        @Positive
        private long negativeTtlSeconds = 30;

        public long getDefaultTtlSeconds() {
            return defaultTtlSeconds;
        }

        public void setDefaultTtlSeconds(long defaultTtlSeconds) {
            this.defaultTtlSeconds = defaultTtlSeconds;
        }

        public long getNegativeTtlSeconds() {
            return negativeTtlSeconds;
        }

        public void setNegativeTtlSeconds(long negativeTtlSeconds) {
            this.negativeTtlSeconds = negativeTtlSeconds;
        }
    }

    public static class Analytics {

        @Positive
        private long publishTimeoutMs = 50;

        public long getPublishTimeoutMs() {
            return publishTimeoutMs;
        }

        public void setPublishTimeoutMs(long publishTimeoutMs) {
            this.publishTimeoutMs = publishTimeoutMs;
        }
    }
}
