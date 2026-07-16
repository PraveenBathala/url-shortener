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
  +--> POST /api/v1/urls ----> CreationController
  |                               |
  |                          UrlValidation
  |                               |
  |                          ShortCodeGenerator
  |                               |
  |                          UrlCreationService
  |                               |
  |                          ShortUrlRepository --> PostgreSQL
  |                               |
  |                          (optional cache warm)
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
  |                          RedirectEventPublisher (best-effort)
  |
  +--> DELETE /api/v1/urls/{code} --> UrlManagementService
                                         |
                                    soft-disable + cache invalidate
```

## Creation path

1. Client submits destination URL and optional expiration.
2. Controller binds/validates the request DTO.
3. Destination validator enforces HTTP/HTTPS structural rules (no network fetch).
4. Service generates a Base62 short code via `SecureRandom`.
5. Service inserts into PostgreSQL.
6. Unique constraint detects collisions; service retries system-generated codes up to a configured limit.
7. Response returns `201 Created` with short URL metadata.

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
RedirectService --> RedirectEventPublisher --> in-process sink / future broker
```

- Minimal event fields (short code, timestamp, outcome)
- No raw IP unless explicitly required later
- Duplicate-event and loss behavior documented in operations docs

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
