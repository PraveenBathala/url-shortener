package com.example.urlshortener.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RedirectAnalyticsRepository extends JpaRepository<RedirectAnalytics, String> {
}
