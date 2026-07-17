# Interview Notes

## How did you decompose the problem?

Separated create, redirect, analytics, cache, management, security, and agentic safety into layered components. Optimized the redirect hot path while keeping PostgreSQL authoritative.

## Assumptions

Redirects dominate traffic (~100:1 vs create); Redis is cache-only; analytics are eventually consistent; management APIs use a shared API key for the MVP (OIDC later for multi-tenant). See `docs/assumptions.md` and quantified NFRs in `docs/requirements.md`.

## Why PostgreSQL?

Strong constraints, Flyway migrations, transactional inserts for collision handling, mature Testcontainers support, clear source-of-truth story.

## Why Redis? Why not authoritative?

Redirect amplification. Redis failure must not take down valid links while PostgreSQL is healthy.

## Why HTTP 302?

Links can expire or be disabled; permanent redirects risk sticky client/intermediary caches.

## How are collisions handled?

SecureRandom Base62 generation → insert → DB unique constraint detects collision → bounded retry for system-generated codes only. Generation alone never claims uniqueness.

## Redis failure?

Cache adapter catches failures, records metrics, and falls back to PostgreSQL.

## Analytics failure?

Best-effort publisher with timeout; redirect continues; failure metric incremented; events (and counters) may lag or be lost. HTTP analytics reads aggregates from `redirect_analytics`.

## SSRF avoidance?

Structural validation plus agentic host heuristics; **no network fetch** of destinations in the request path. DNS reputation scanning is deferred and must be isolated if added.

## What is the agentic piece?

`UrlSafetyAgent` runs a plan → act → observe → decide loop over deterministic tools (scheme, host risk, length). HIGH risk can block create. No external LLM in MVP so tests stay hermetic. See `docs/agentic.md`.

## Security model?

- Public: redirects, health, OpenAPI UI
- Protected with `X-API-Key`: create, bulk, disable, analytics
- Write rate limiting (in-memory, per instance)
- Privacy-safe audit logs and JSON application logs

## Bulk create?

Per-item partial success (`CREATED` / `FAILED`) so one bad URL does not discard the batch.

## Why Testcontainers?

Validates real PostgreSQL/Flyway/constraint/Redis behavior that H2 can hide.

## AI usage

Cursor accelerated scaffolding and gap closure under `instructions.md`. Human owns decisions, validation, Git, and submission. Real accept/reject examples are in `docs/ai-usage-log.md` (including rejecting remote LLM planner and all-or-nothing bulk).

## What is not production-ready?

- Shared API key (not OIDC / per-user RBAC)
- In-memory rate limiter (not multi-instance)
- No remote malware/phishing reputation API
- Analytics sink is in-process (replaceable; counters persisted)
- Single-region MVP; custom aliases deferred

## Scaling path

Stateless app replicas → shared Redis → PostgreSQL primary (+ replicas later) → Redis-backed rate limits → durable event bus if analytics volume requires it → OIDC for multi-tenant brokerage.
