package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.urlshortener.api.dto.BulkCreateShortUrlRequest;
import com.example.urlshortener.api.dto.BulkCreateShortUrlResponse;
import com.example.urlshortener.api.dto.CreateShortUrlRequest;
import com.example.urlshortener.api.dto.ShortUrlResponse;
import com.example.urlshortener.api.error.BulkLimitExceededException;
import com.example.urlshortener.api.error.InvalidDestinationUrlException;
import com.example.urlshortener.audit.AuditLogger;
import com.example.urlshortener.config.AppProperties;
import com.example.urlshortener.domain.ShortUrlStatus;

@ExtendWith(MockitoExtension.class)
class BulkUrlCreationServiceTest {

    @Mock
    private UrlCreationService urlCreationService;

    @Mock
    private AuditLogger auditLogger;

    private BulkUrlCreationService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getBulk().setMaxBatchSize(2);
        service = new BulkUrlCreationService(urlCreationService, properties, auditLogger);
    }

    @Test
    void returnsPartialSuccess() {
        when(urlCreationService.create(any()))
                .thenReturn(new ShortUrlResponse(
                        "code001",
                        "http://localhost:8080/code001",
                        "https://example.com/a",
                        Instant.parse("2026-07-16T20:00:00Z"),
                        null,
                        ShortUrlStatus.ACTIVE))
                .thenThrow(new InvalidDestinationUrlException("bad"));

        BulkCreateShortUrlResponse response = service.createBulk(new BulkCreateShortUrlRequest(List.of(
                new CreateShortUrlRequest("https://example.com/a", null),
                new CreateShortUrlRequest("javascript:alert(1)", null))));

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).status()).isEqualTo("CREATED");
        assertThat(response.results().get(1).status()).isEqualTo("FAILED");
        assertThat(response.results().get(1).errorCode()).isEqualTo("INVALID_DESTINATION_URL");
    }

    @Test
    void rejectsOversizedBatch() {
        assertThatThrownBy(() -> service.createBulk(new BulkCreateShortUrlRequest(List.of(
                        new CreateShortUrlRequest("https://example.com/a", null),
                        new CreateShortUrlRequest("https://example.com/b", null),
                        new CreateShortUrlRequest("https://example.com/c", null)))))
                .isInstanceOf(BulkLimitExceededException.class);
    }
}
