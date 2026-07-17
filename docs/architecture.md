# Architecture

## Overview

The URL shortener is a single Spring Boot application with clear layered boundaries:

- Controllers handle HTTP concerns only
- Services own business rules
- Repositories persist to PostgreSQL
- Cache adapters encapsulate Redis
- Event publishers deliver analytics best-effort
- Exception handlers map domain failures to a stable error schema

PostgreSQL is authoritative. Redis accelerates the redirect hot path. Analytics never blocks redirects.

## Component diagram

```text
Client
  |
  +--> POST /api/v1/urls (+ /bulk) --> ApiKey + RateLimit
  |                                      |
  |                                 UrlCreationController
  |                                      |
  |                                 DestinationUrlValidator
  |                                      |
  |                                 UrlSafetyAgent (tools)
  |                                      |
  |                                 UrlCreationService --> PostgreSQL
  |                                      |
  |                                 AuditLogger
  |
  +--> GET /{shortCode} ----> RedirectController
  |                               |
  |                          RedirectService
  |                               |
  |                    +--> Redis cache-aside
  |                    |         |
  |                    +--> PostgreSQL on miss/failure
  |                               |
  |                          302 Location / 4xx
  |                               |
  |                          RedirectEventPublisher --> redirect_analytics
  |
  +--> GET /api/v1/analytics/{code} --> ApiKey --> AnalyticsQueryService
  |
  +--> DELETE /api/v1/urls/{code} --> ApiKey --> UrlManagementService
                                         |
                                    soft-disable + cache invalidate + audit
```

## Creation path

1. Client submits destination URL and optional expiration with `X-API-Key`.
2. Rate limiter and API key filter gate the request.
3. Controller binds/validates the request DTO.
4. Destination validator enforces HTTP/HTTPS structural rules (no network fetch).
5. `UrlSafetyAgent` runs plan→act→observe→decide over safety tools; HIGH risk may block.
6. Service generates a Base62 short code via `SecureRandom`.
7. Service inserts into PostgreSQL.
8. Unique constraint detects collisions; service retries system-generated codes up to a configured limit.
9. Audit log records create; response returns `201 Created`.

## Redirect path

1. Client requests `GET /{shortCode}`.
2. Service validates short-code shape.
3. Cache-aside lookup in Redis for active mappings.
4. On miss or Redis failure, load from PostgreSQL.
5. Evaluate status and expiration.
6. Return `302` with `Location`, or `404`/`410`/`400` as appropriate.
7. Publish analytics event asynchronously with a strict timeout; failures are metered only.

## Analytics path

```text
RedirectService --> RedirectEventPublisher --> log sink + redirect_analytics aggregates
Client --> GET /api/v1/analytics/{code} (API key)
```

- Minimal event fields (short code, timestamp, outcome)
- No raw IP unless explicitly required later
- Aggregate counters power the HTTP analytics API
- Duplicate-event and loss behavior documented in operations docs
- See `docs/agentic.md` for the create-path safety agent

## Database

Flyway migration creates `short_urls` with:

- `short_code` (PK)
- `destination_url`
- `status`
- `created_at`, `updated_at`
- `expires_at` (nullable)
- `version` (optimistic locking)

Analytics events are not stored by synchronously mutating click counters on the mapping row.

## Cache behavior

- Cache active mappings only
- TTL respects expiration when present
- Invalidate on disable or destination/status changes
- Optional short negative caching for unknown codes
- Metrics: hit, miss, failure

## Failure handling

| Dependency | Behavior |
|---|---|
| Redis down | Fall back to PostgreSQL; continue serving redirects |
| Analytics down | Redirect continues; failure metric incremented |
| PostgreSQL down | Creation/redirect fail with controlled 5xx; no silent corruption |
| Collision retries exhausted | Controlled domain error mapped to API response |

## Scaling path (future)

1. Vertical scale application instances behind a load balancer
2. Keep PostgreSQL primary as authority; add read replicas if redirect DB load grows
3. Expand Redis as shared cache across instances
4. Replace in-process analytics publisher with a durable broker if volume requires it
5. Consider sequence/Snowflake codes or custom aliases only after uniqueness and abuse controls are redesigned

## Package layout (planned)

```text
com.example.urlshortener
  api            # controllers, DTOs, error handling
  domain         # entities, enums
  persistence    # repositories
  service        # business services
  generation     # short-code generator
  validation     # destination URL validation
  cache          # Redis adapters
  analytics      # event publisher
  config         # properties and infrastructure wiring
  metrics        # domain metrics helpers
```
