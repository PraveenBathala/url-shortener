# Tradeoffs

## PostgreSQL versus distributed key-value stores

**Chosen:** PostgreSQL as source of truth.

**Why:** Strong constraints, Flyway migrations, transactional inserts for collision handling, mature Testcontainers support, and clear operational model for an interviewable MVP.

**Tradeoff:** Higher latency than a pure in-memory key-value store for hot redirects; mitigated by Redis cache-aside.

## Random Base62 codes versus sequence-derived codes

**Chosen:** SecureRandom Base62 with DB uniqueness and bounded retry.

**Why:** No central sequence dependency, codes are non-enumerable by default, simple to reason about.

**Tradeoff:** Collisions are possible (not guaranteed unique by generation alone). Database constraint is the final uniqueness guarantee.

## HTTP 302 versus 301/308

**Chosen:** HTTP 302 Found by default.

**Why:** Clients and intermediaries are less likely to permanently cache redirects, which matters when links can expire or be disabled.

**Tradeoff:** Slightly more redirect load versus permanent redirects; acceptable for mutable short links.

## Cache-aside versus write-through / Redis-as-authority

**Chosen:** Cache-aside with PostgreSQL authority.

**Why:** Redis failure must not take down valid redirects. Simpler consistency story for an MVP.

**Tradeoff:** Cache misses hit the database; possible brief stale reads if invalidation fails (invalidation is explicit on disable).

## Synchronous versus asynchronous analytics

**Chosen:** Asynchronous / best-effort event publishing with timeout.

**Why:** Redirect availability must not depend on analytics. Click counters are eventually consistent by design.

**Tradeoff:** Possible event loss under publisher failure; documented and metered.

## Testcontainers versus H2

**Chosen:** PostgreSQL Testcontainers for integration tests.

**Why:** Validates real SQL, constraints, and Flyway behavior. H2 can hide PostgreSQL-specific issues.

**Tradeoff:** Requires Docker; slower than H2-only unit tests. Unit tests still cover pure logic without containers.

## Custom aliases versus generated codes only

**Decision (Phase 10): Deferred for MVP.**

Custom aliases were reviewed against the current API. Implementing them now would add alias character rules, min/max length, reserved-word lists, `409` conflict semantics, concurrency tests, and idempotency definitions without improving the core create/redirect evidence required by the rubric.

**Deferred behavior (if added later):**

- Validate alias characters and length
- Reserve system words (`api`, `actuator`, `health`, etc.)
- Return `409` on conflict
- Do not retry custom aliases
- Document idempotency explicitly

MVP ships generated Base62 codes only.

## Bulk create: partial success versus all-or-nothing

**Chosen:** Per-item results with HTTP 200 (`CREATED` / `FAILED` entries).

**Why:** Callers can retry only failed items; one bad URL should not discard a whole marketing batch.

**Tradeoff:** Clients must inspect `results[]` instead of relying on a single status code.

## API key versus full OAuth

**Chosen:** Shared `X-API-Key` for management APIs.

**Why:** Closes the unauthenticated create/disable gap with minimal ceremony; interviewable and env-configurable.

**Tradeoff:** No per-user identity or fine-grained RBAC — document upgrade path to OIDC for brokerage multi-tenant use.

## Deterministic agent tools versus external LLM planner

**Chosen:** Deterministic multi-tool `UrlSafetyAgent` (no remote LLM call in MVP).

**Why:** Hermetic tests, no API keys/cost, SSRF-safe. Still demonstrates plan→act→observe→decide.

**Tradeoff:** Less flexible than an LLM planner; extension point documented in `docs/agentic.md`.

## Strong versus eventual consistency

**Chosen:** Strong consistency for mapping create/read against PostgreSQL; eventual consistency for analytics and cache population.

**Why:** Users must resolve the same destination that was stored. Analytics and cache are performance/observability concerns, not authority.
