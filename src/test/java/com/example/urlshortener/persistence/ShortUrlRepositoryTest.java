package com.example.urlshortener.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.urlshortener.domain.ShortUrl;
import com.example.urlshortener.domain.ShortUrlStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ShortUrlRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("urlshortener")
            .withUsername("urlshortener")
            .withPassword("urlshortener");

    @Autowired
    private ShortUrlRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsAndLoadsShortUrl() {
        Instant now = Instant.parse("2026-07-16T20:00:00Z");
        ShortUrl saved = repository.saveAndFlush(new ShortUrl(
                "aB7xK9P",
                "https://example.com/products/123",
                ShortUrlStatus.ACTIVE,
                now,
                now,
                Instant.parse("2027-01-01T00:00:00Z")));

        ShortUrl loaded = repository.findById("aB7xK9P").orElseThrow();

        assertThat(loaded.getShortCode()).isEqualTo(saved.getShortCode());
        assertThat(loaded.getDestinationUrl()).isEqualTo("https://example.com/products/123");
        assertThat(loaded.getStatus()).isEqualTo(ShortUrlStatus.ACTIVE);
        assertThat(loaded.getCreatedAt()).isEqualTo(now);
        assertThat(loaded.getExpiresAt()).isEqualTo(Instant.parse("2027-01-01T00:00:00Z"));
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    void rejectsDuplicateShortCode() {
        Instant now = Instant.parse("2026-07-16T20:00:00Z");
        repository.saveAndFlush(new ShortUrl(
                "dupCode1",
                "https://example.com/one",
                ShortUrlStatus.ACTIVE,
                now,
                now,
                null));
        entityManager.clear();

        assertThatThrownBy(() -> jdbcTemplate.update(
                        """
                        INSERT INTO short_urls
                        (short_code, destination_url, status, created_at, updated_at, expires_at, version)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        "dupCode1",
                        "https://example.com/two",
                        "ACTIVE",
                        java.sql.Timestamp.from(now),
                        java.sql.Timestamp.from(now),
                        null,
                        0L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
