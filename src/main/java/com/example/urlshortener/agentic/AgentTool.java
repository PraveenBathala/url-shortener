package com.example.urlshortener.agentic;

/**
 * A single tool in the URL safety agent toolbelt.
 * Tools observe one facet of a destination and return structured findings.
 */
public interface AgentTool {

    String name();

    ToolObservation observe(String destinationUrl);
}
