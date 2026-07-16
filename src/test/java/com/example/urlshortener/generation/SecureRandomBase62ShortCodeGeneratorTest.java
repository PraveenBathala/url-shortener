package com.example.urlshortener.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SecureRandomBase62ShortCodeGeneratorTest {

    @Test
    void generatesConfiguredLengthBase62Code() {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.setSeed(1L);
        SecureRandomBase62ShortCodeGenerator generator =
                new SecureRandomBase62ShortCodeGenerator(7, secureRandom);
        generator.validateConfiguration();

        String code = generator.generate();

        assertThat(code).hasSize(7);
        assertThat(code).matches("[0-9a-zA-Z]{7}");
    }

    @Test
    void producesVaryingCodesAcrossCalls() {
        SecureRandomBase62ShortCodeGenerator generator =
                new SecureRandomBase62ShortCodeGenerator(7, new SecureRandom());
        generator.validateConfiguration();

        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            codes.add(generator.generate());
        }

        assertThat(codes.size()).isGreaterThan(1);
    }

    @Test
    void rejectsInvalidConfiguredLengthAtStartup() {
        SecureRandomBase62ShortCodeGenerator generator =
                new SecureRandomBase62ShortCodeGenerator(3, new SecureRandom());

        assertThatThrownBy(generator::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("between 4 and 16");
    }
}
