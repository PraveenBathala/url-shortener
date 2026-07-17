package com.example.urlshortener.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.api.dto.BulkCreateShortUrlRequest;
import com.example.urlshortener.api.dto.BulkCreateShortUrlResponse;
import com.example.urlshortener.api.dto.CreateShortUrlRequest;
import com.example.urlshortener.api.dto.ShortUrlResponse;
import com.example.urlshortener.service.BulkUrlCreationService;
import com.example.urlshortener.service.UrlCreationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URL Creation")
@SecurityRequirement(name = "ApiKeyAuth")
public class UrlCreationController {

    private final UrlCreationService urlCreationService;
    private final BulkUrlCreationService bulkUrlCreationService;

    public UrlCreationController(
            UrlCreationService urlCreationService, BulkUrlCreationService bulkUrlCreationService) {
        this.urlCreationService = urlCreationService;
        this.bulkUrlCreationService = bulkUrlCreationService;
    }

    @PostMapping
    @Operation(summary = "Create a short URL")
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = urlCreationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create multiple short URLs (partial success per item)")
    public ResponseEntity<BulkCreateShortUrlResponse> createBulk(
            @Valid @RequestBody BulkCreateShortUrlRequest request) {
        return ResponseEntity.ok(bulkUrlCreationService.createBulk(request));
    }
}
