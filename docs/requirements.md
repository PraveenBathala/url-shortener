# Requirements

## Product goals

Build a production-oriented URL shortener that:

1. Creates short URLs from destination URLs
2. Resolves short codes with HTTP 302 redirects
3. Persists mappings in PostgreSQL
4. Caches active mappings in Redis (cache-aside)
5. Publishes redirect analytics asynchronously without blocking redirects
6. Exposes health and metrics endpoints
7. Ships as a tested executable JAR

## Functional requirements

| ID | Requirement | Acceptance criteria |
|---|---|---|
| FR-01 | Create short URL | `POST /api/v1/urls` returns `201` with short code, short URL, destination, timestamps, and status |
| FR-02 | Resolve short URL | `GET /{shortCode}` returns `302` with `Location` for active, unexpired mappings |
| FR-03 | Validate destination | Accept only HTTP/HTTPS with valid host, reject blank, malformed, oversized, and control-character input |
| FR-04 | Generate short codes | SecureRandom Base62 codes (default length 7), uniqueness enforced by DB constraint with bounded retry |
| FR-05 | Optional expiration | Future `expiresAt` accepted; expired codes return `410 Gone` |
| FR-06 | Disable links | Soft-disable via `DELETE /api/v1/urls/{shortCode}` returns `204`; disabled codes return `410` |
| FR-07 | Unknown codes | Unknown short codes return `404` |
| FR-08 | Malformed codes | Malformed short codes return `400` |
| FR-09 | Consistent errors | Stable JSON error schema with timestamp, status, errorCode, message, path, requestId |
| FR-10 | Persistence | PostgreSQL is source of truth; Flyway manages schema |
| FR-11 | Caching | Redis cache-aside for redirects; PostgreSQL fallback when Redis unavailable |
| FR-12 | Analytics | Best-effort redirect event publishing; redirect success independent of analytics |
| FR-13 | Observability | Actuator health, Micrometer metrics, structured logs without sensitive data |
| FR-14 | Local ops | Docker Compose for PostgreSQL/Redis; executable JAR for the application |

## Non-functional requirements (quantified targets)

These are **design targets**, not measured production SLOs.

| ID | Requirement | Target / assumption |
|---|---|---|
| NFR-01 | Redirect path optimized | Redirect:create traffic ratio ≈ 100:1 |
| NFR-02 | Cached redirect latency | p99 ≤ 20 ms for cache hits (local/single-region target) |
| NFR-03 | Uncached redirect latency | p99 ≤ 100 ms when PostgreSQL is healthy |
| NFR-04 | Create latency | p99 ≤ 200 ms excluding client network |
| NFR-05 | Availability under cache failure | Valid links remain available if PostgreSQL is healthy; redirect availability target 99.99% |
| NFR-06 | Create/disable abuse control | Default 60 authenticated write requests/minute/client |
| NFR-07 | Privacy-safe logging | Do not log full destinations, secrets, or PII; JSON logs for ELK/Loki |
| NFR-08 | Scale assumption (MVP) | Single region, ~1k redirects/sec peak with Redis cache-aside (not load-tested here) |
| NFR-09 | Testability | Unit, controller, and PostgreSQL/Redis Testcontainers integration tests |
| NFR-10 | Configurability | Secrets and environment settings via environment variables |

## Out of scope for MVP

- OAuth/OIDC multi-user identity
- Distributed (Redis) rate limiting across many instances
- Remote malware/phishing reputation APIs
- Kafka or other event streaming platforms
- Kubernetes / multi-region deployment
- Custom aliases (evaluated in Phase 10; deferred)

## Success definition

- Full automated test suite passes
- Executable JAR builds and runs
- Create, redirect, validation, expiration/disable, Redis fallback, health, and metrics verified
- Documentation supports rubric evidence and interview discussion
