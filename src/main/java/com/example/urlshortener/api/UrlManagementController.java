package com.example.urlshortener.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.service.UrlManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URL Management")
@SecurityRequirement(name = "ApiKeyAuth")
public class UrlManagementController {

    private final UrlManagementService urlManagementService;

    public UrlManagementController(UrlManagementService urlManagementService) {
        this.urlManagementService = urlManagementService;
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Soft-disable a short URL")
    public ResponseEntity<Void> disable(@PathVariable String shortCode) {
        urlManagementService.disable(shortCode);
        return ResponseEntity.noContent().build();
    }
}
