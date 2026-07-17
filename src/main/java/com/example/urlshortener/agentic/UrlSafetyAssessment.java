package com.example.urlshortener.agentic;

import java.util.List;

public record UrlSafetyAssessment(
        RiskLevel overallRisk, String decision, List<ToolObservation> observations, List<String> reasoningTrace) {
}
