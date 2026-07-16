package com.example.urlshortener.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.urlshortener.api.error.InvalidDestinationUrlException;
import com.example.urlshortener.config.AppProperties;

class DestinationUrlValidatorTest {

    private DestinationUrlValidator validator;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getDestinationUrl().setMaxLength(2048);
        validator = new DestinationUrlValidator(properties);
    }

    @Test
    void acceptsHttpAndHttpsUrls() {
        assertThat(validator.validate("https://example.com/products/123"))
                .isEqualTo("https://example.com/products/123");
        assertThat(validator.validate(" http://example.com "))
                .isEqualTo("http://example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "file:///etc/passwd",
            "data:text/plain,hi",
            "ftp://example.com/file"
    })
    void rejectsUnsupportedSchemes(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(InvalidDestinationUrlException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void rejectsBlankAndMissingHost() {
        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(InvalidDestinationUrlException.class);
        assertThatThrownBy(() -> validator.validate("https:///path-only"))
                .isInstanceOf(InvalidDestinationUrlException.class)
                .hasMessageContaining("host");
    }

    @Test
    void rejectsControlCharactersAndOversizedInput() {
        assertThatThrownBy(() -> validator.validate("https://example.com/\npath"))
                .isInstanceOf(InvalidDestinationUrlException.class)
                .hasMessageContaining("control characters");

        AppProperties tight = new AppProperties();
        tight.getDestinationUrl().setMaxLength(20);
        DestinationUrlValidator tightValidator = new DestinationUrlValidator(tight);

        assertThatThrownBy(() -> tightValidator.validate("https://example.com/too-long-for-limit"))
                .isInstanceOf(InvalidDestinationUrlException.class)
                .hasMessageContaining("maximum length");
    }

    @Test
    void rejectsMalformedUri() {
        assertThatThrownBy(() -> validator.validate("https://exa mple.com"))
                .isInstanceOf(InvalidDestinationUrlException.class)
                .hasMessageContaining("malformed");
    }
}
