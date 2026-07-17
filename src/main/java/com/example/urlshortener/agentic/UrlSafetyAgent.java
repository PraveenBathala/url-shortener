package com.example.urlshortener.agentic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.urlshortener.config.AppProperties;

/**
 * Lightweight agentic orchestrator for destination URL safety.
 *
 * <p>Loop: plan tools → observe each tool → reason over findings → decide ALLOW/BLOCK.
 * No external LLM call is required for the MVP; tools are deterministic and SSRF-safe
 * (no destination fetch). This keeps tests hermetic while demonstrating an agent pattern
 * that can later swap in an LLM planner behind the same interface.
 */
@Component
public class UrlSafetyAgent {

    private static final Logger log = LoggerFactory.getLogger(UrlSafetyAgent.class);

    private final List<AgentTool> tools;
    private final AppProperties appProperties;

    public UrlSafetyAgent(List<AgentTool> tools, AppProperties appProperties) {
        this.tools = List.copyOf(tools);
        this.appProperties = appProperties;
    }

    public UrlSafetyAssessment assess(String destinationUrl) {
        List<String> trace = new ArrayList<>();
        trace.add("plan: run " + tools.size() + " safety tools without network fetch");

        List<ToolObservation> observations = new ArrayList<>();
        for (AgentTool tool : tools) {
            trace.add("act: invoke tool=" + tool.name());
            ToolObservation observation = tool.observe(destinationUrl);
            observations.add(observation);
            trace.add("observe: tool=" + observation.toolName()
                    + " risk=" + observation.riskLevel()
                    + " finding=" + observation.finding());
        }

        RiskLevel overall = observations.stream()
                .map(ToolObservation::riskLevel)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(RiskLevel.LOW);

        String decision;
        if (overall == RiskLevel.HIGH && appProperties.getAgentic().isBlockHighRisk()) {
            decision = "BLOCK";
            trace.add("decide: BLOCK due to HIGH risk finding");
        } else {
            decision = "ALLOW";
            trace.add("decide: ALLOW overallRisk=" + overall);
        }

        log.info(
                "operation=urlSafetyAgent decision={} overallRisk={} toolCount={}",
                decision,
                overall,
                observations.size());
        return new UrlSafetyAssessment(overall, decision, List.copyOf(observations), List.copyOf(trace));
    }
}
