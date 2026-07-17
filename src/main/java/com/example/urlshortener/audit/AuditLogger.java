package com.example.urlshortener.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.example.urlshortener.api.RequestIdFilter;

/**
 * Privacy-safe audit trail for sensitive management actions.
 * Logs action + short-code length only — never destinations, keys, or bodies.
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    public void record(String action, String shortCode, String outcome) {
        log.info(
                "operation=audit action={} outcome={} shortCodeLength={} requestId={}",
                action,
                outcome,
                shortCode == null ? 0 : shortCode.length(),
                MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }

    public void recordBulk(String action, int itemCount, int successCount, int failureCount) {
        log.info(
                "operation=audit action={} outcome=completed itemCount={} successCount={} failureCount={} requestId={}",
                action,
                itemCount,
                successCount,
                failureCount,
                MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));
    }
}
