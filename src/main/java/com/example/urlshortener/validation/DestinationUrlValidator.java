package com.example.urlshortener.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.api.error.InvalidDestinationUrlException;

@Component
public class DestinationUrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final int maxLength;

    public DestinationUrlValidator(AppProperties appProperties) {
        this.maxLength = appProperties.getDestinationUrl().getMaxLength();
    }

    public String validate(String rawDestinationUrl) {
        if (rawDestinationUrl == null || rawDestinationUrl.isBlank()) {
            throw new InvalidDestinationUrlException("Destination URL is required");
        }

        String destinationUrl = rawDestinationUrl.trim();
        if (destinationUrl.length() > maxLength) {
            throw new InvalidDestinationUrlException(
                    "Destination URL exceeds maximum length of " + maxLength);
        }

        if (containsControlCharacters(destinationUrl)) {
            throw new InvalidDestinationUrlException("Destination URL contains control characters");
        }

        final URI uri;
        try {
            uri = new URI(destinationUrl);
        } catch (URISyntaxException ex) {
            throw new InvalidDestinationUrlException("Destination URL is malformed");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new InvalidDestinationUrlException("Destination URL scheme must be http or https");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidDestinationUrlException("Destination URL must include a valid host");
        }

        // Structural validation only — no network fetch (SSRF avoidance for MVP).
        return destinationUrl;
    }

    private static boolean containsControlCharacters(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
