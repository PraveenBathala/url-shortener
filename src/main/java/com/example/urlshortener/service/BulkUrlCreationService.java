package com.example.urlshortener.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.urlshortener.api.dto.BulkCreateItemResult;
import com.example.urlshortener.api.dto.BulkCreateShortUrlRequest;
import com.example.urlshortener.api.dto.BulkCreateShortUrlResponse;
import com.example.urlshortener.api.dto.ShortUrlResponse;
import com.example.urlshortener.api.error.ApplicationException;
import com.example.urlshortener.api.error.BulkLimitExceededException;
import com.example.urlshortener.audit.AuditLogger;
import com.example.urlshortener.config.AppProperties;

@Service
public class BulkUrlCreationService {

    private final UrlCreationService urlCreationService;
    private final AppProperties appProperties;
    private final AuditLogger auditLogger;

    public BulkUrlCreationService(
            UrlCreationService urlCreationService, AppProperties appProperties, AuditLogger auditLogger) {
        this.urlCreationService = urlCreationService;
        this.appProperties = appProperties;
        this.auditLogger = auditLogger;
    }

    public BulkCreateShortUrlResponse createBulk(BulkCreateShortUrlRequest request) {
        int max = appProperties.getBulk().getMaxBatchSize();
        if (request.urls().size() > max) {
            throw new BulkLimitExceededException(max);
        }
        List<BulkCreateItemResult> results = new ArrayList<>(request.urls().size());
        int success = 0;
        int failure = 0;
        for (int i = 0; i < request.urls().size(); i++) {
            try {
                ShortUrlResponse created = urlCreationService.create(request.urls().get(i));
                results.add(BulkCreateItemResult.created(i, created));
                success++;
            } catch (ApplicationException ex) {
                results.add(BulkCreateItemResult.failed(i, ex.getErrorCode().name(), ex.getMessage()));
                failure++;
            } catch (RuntimeException ex) {
                results.add(BulkCreateItemResult.failed(i, "INTERNAL_ERROR", "An unexpected error occurred"));
                failure++;
            }
        }
        auditLogger.recordBulk("BULK_CREATE_URL", request.urls().size(), success, failure);
        return new BulkCreateShortUrlResponse(results);
    }
}
