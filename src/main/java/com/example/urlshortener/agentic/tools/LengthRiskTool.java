package com.example.urlshortener.agentic.tools;

import org.springframework.stereotype.Component;

import com.example.urlshortener.agentic.AgentTool;
import com.example.urlshortener.agentic.RiskLevel;
import com.example.urlshortener.agentic.ToolObservation;
import com.example.urlshortener.config.AppProperties;

@Component
public class LengthRiskTool implements AgentTool {

    private final AppProperties appProperties;

    public LengthRiskTool(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String name() {
        return "length_risk";
    }

    @Override
    public ToolObservation observe(String destinationUrl) {
        int length = destinationUrl == null ? 0 : destinationUrl.length();
        int max = appProperties.getDestinationUrl().getMaxLength();
        if (length > max) {
            return new ToolObservation(name(), RiskLevel.HIGH, "exceeds max length");
        }
        if (length > max * 0.8) {
            return new ToolObservation(name(), RiskLevel.MEDIUM, "near max length");
        }
        return new ToolObservation(name(), RiskLevel.LOW, "length within bounds");
    }
}
