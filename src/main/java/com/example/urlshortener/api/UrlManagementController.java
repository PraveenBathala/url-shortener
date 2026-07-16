package com.example.urlshortener.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.service.UrlManagementService;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlManagementController {

    private final UrlManagementService urlManagementService;

    public UrlManagementController(UrlManagementService urlManagementService) {
        this.urlManagementService = urlManagementService;
    }

    /**
     * Soft-disable endpoint. Requires authentication before production exposure.
     */
    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> disable(@PathVariable String shortCode) {
        urlManagementService.disable(shortCode);
        return ResponseEntity.noContent().build();
    }
}
