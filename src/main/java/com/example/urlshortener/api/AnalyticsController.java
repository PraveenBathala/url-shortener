package com.example.urlshortener.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.api.dto.AnalyticsResponse;
import com.example.urlshortener.service.AnalyticsQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics")
@SecurityRequirement(name = "ApiKeyAuth")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get redirect analytics for a short code")
    public AnalyticsResponse getAnalytics(@PathVariable String shortCode) {
        return analyticsQueryService.getAnalytics(shortCode);
    }
}
