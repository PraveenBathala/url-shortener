# Agentic URL Safety

## Why an agent here

Assessment gaps called out missing agentic artifacts. Rather than bolting on an unrelated chatbot, the create path uses a small **tool-using agent** that:

1. **Plans** which safety tools to run
2. **Acts** by invoking each tool
3. **Observes** structured findings
4. **Decides** ALLOW or BLOCK

This keeps the core product honest (URL shortening) while demonstrating an agentic control loop that is testable without external LLM APIs.

## Components

| Piece | Role |
|---|---|
| `UrlSafetyAgent` | Orchestrator (plan → act → observe → decide) |
| `SchemeSafetyTool` | Scheme risk (https/http/other) |
| `HostRiskTool` | Restricted hosts / literal private IPs (no DNS fetch) |
| `LengthRiskTool` | Oversized / near-limit URLs |
| `UrlSafetyAssessment` | Decision + reasoning trace for audit/debug |

## SSRF posture

Tools never open network connections to the destination. Host checks use literal IP parsing and hostname allow/deny heuristics only. Full reputation scanning remains a future isolated component (see `docs/security.md`).

## Configuration

```text
APP_AGENTIC_ENABLED=true
APP_AGENTIC_BLOCK_HIGH_RISK=true
```

When disabled, structural `DestinationUrlValidator` still runs.

## Extension path

Swap or wrap the planner with an LLM that selects tools dynamically, while keeping the same `AgentTool` interface and SSRF constraints.
