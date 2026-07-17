package com.example.urlshortener.analytics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsPersistenceService {

    private final RedirectAnalyticsRepository analyticsRepository;

    public AnalyticsPersistenceService(RedirectAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional
    public void record(RedirectEvent event) {
        RedirectAnalytics analytics = analyticsRepository
                .findById(event.shortCode())
                .orElseGet(() -> new RedirectAnalytics(event.shortCode(), event.occurredAt()));
        analytics.record(event.outcome(), event.occurredAt());
        analyticsRepository.save(analytics);
    }
}
