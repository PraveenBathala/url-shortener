package com.example.urlshortener.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.urlshortener.agentic.tools.HostRiskTool;
import com.example.urlshortener.agentic.tools.LengthRiskTool;
import com.example.urlshortener.agentic.tools.SchemeSafetyTool;
import com.example.urlshortener.config.AppProperties;

class UrlSafetyAgentTest {

    private UrlSafetyAgent agent;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getAgentic().setEnabled(true);
        properties.getAgentic().setBlockHighRisk(true);
        agent = new UrlSafetyAgent(
                List.of(new SchemeSafetyTool(), new HostRiskTool(), new LengthRiskTool(properties)),
                properties);
    }

    @Test
    void allowsHttpsPublicHost() {
        UrlSafetyAssessment assessment = agent.assess("https://example.com/path");
        assertThat(assessment.decision()).isEqualTo("ALLOW");
        assertThat(assessment.overallRisk()).isEqualTo(RiskLevel.LOW);
        assertThat(assessment.reasoningTrace()).isNotEmpty();
        assertThat(assessment.observations()).hasSize(3);
    }

    @Test
    void blocksMetadataHost() {
        UrlSafetyAssessment assessment = agent.assess("https://169.254.169.254/latest/meta-data");
        assertThat(assessment.decision()).isEqualTo("BLOCK");
        assertThat(assessment.overallRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void marksPlainHttpAsMediumButAllows() {
        UrlSafetyAssessment assessment = agent.assess("http://example.com");
        assertThat(assessment.overallRisk()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(assessment.decision()).isEqualTo("ALLOW");
    }
}
