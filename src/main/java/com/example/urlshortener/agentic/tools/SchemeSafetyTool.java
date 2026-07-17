package com.example.urlshortener.agentic.tools;

import java.net.URI;

import org.springframework.stereotype.Component;

import com.example.urlshortener.agentic.AgentTool;
import com.example.urlshortener.agentic.RiskLevel;
import com.example.urlshortener.agentic.ToolObservation;

@Component
public class SchemeSafetyTool implements AgentTool {

    @Override
    public String name() {
        return "scheme_safety";
    }

    @Override
    public ToolObservation observe(String destinationUrl) {
        try {
            URI uri = URI.create(destinationUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if ("https".equals(scheme)) {
                return new ToolObservation(name(), RiskLevel.LOW, "https scheme");
            }
            if ("http".equals(scheme)) {
                return new ToolObservation(name(), RiskLevel.MEDIUM, "cleartext http scheme");
            }
            return new ToolObservation(name(), RiskLevel.HIGH, "unsupported or missing scheme");
        } catch (IllegalArgumentException ex) {
            return new ToolObservation(name(), RiskLevel.HIGH, "unparseable uri");
        }
    }
}
