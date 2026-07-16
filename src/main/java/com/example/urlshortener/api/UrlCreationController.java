package com.example.urlshortener.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.api.dto.CreateShortUrlRequest;
import com.example.urlshortener.api.dto.ShortUrlResponse;
import com.example.urlshortener.service.UrlCreationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlCreationController {

    private final UrlCreationService urlCreationService;

    public UrlCreationController(UrlCreationService urlCreationService) {
        this.urlCreationService = urlCreationService;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse response = urlCreationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
