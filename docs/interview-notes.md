# Interview Notes

## How did you decompose the problem?

Separated create, redirect, analytics, cache, and management into layered components. Optimized the redirect hot path while keeping PostgreSQL authoritative.

## Assumptions

Redirects dominate traffic; Redis is cache-only; analytics are eventually consistent; auth can be deferred for demo but not for production management APIs. See `docs/assumptions.md`.

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

Best-effort publisher with timeout; redirect continues; failure metric incremented; events may be lost.

## SSRF avoidance?

Structural validation only; no network fetch of destinations in the request path.

## Why Testcontainers?

Validates real PostgreSQL/Flyway/constraint behavior that H2 can hide.

## AI usage

Cursor accelerated scaffolding and implementation under `instructions.md`. Human owns decisions, validation, Git, and submission. Real accept/reject examples are in `docs/ai-usage-log.md`.

## What is not production-ready?

AuthN/Z, rate limiting, malware/phishing detection, durable analytics broker, multi-region design.

## Scaling path

Stateless app replicas → shared Redis → PostgreSQL primary (+ replicas later) → durable event bus if analytics volume requires it.
