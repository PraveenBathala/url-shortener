# Data Model

## Table: `short_urls`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `short_code` | `VARCHAR(16)` | Primary key | System-generated Base62 code (default length 7) |
| `destination_url` | `TEXT` | NOT NULL, non-blank check | Validated HTTP/HTTPS destination |
| `status` | `VARCHAR(32)` | NOT NULL, `ACTIVE`/`DISABLED` | Soft-disable supported |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | UTC |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | UTC; updated on disable |
| `expires_at` | `TIMESTAMPTZ` | NULL | Optional future expiration |
| `version` | `BIGINT` | NOT NULL, default 0 | JPA `@Version` optimistic locking |

## Indexes

| Index | Definition | Reason |
|---|---|---|
| `pk_short_urls` | Primary key on `short_code` | Lookup by code; uniqueness guarantee |
| `idx_short_urls_expires_at_active` | Partial index on `expires_at` where `status = 'ACTIVE' AND expires_at IS NOT NULL` | Supports expiry cleanup / operational queries |

## Uniqueness

Random generation does **not** guarantee uniqueness. The primary key constraint is the final uniqueness guarantee. Creation retries only for system-generated codes.

## Analytics

Click/redirect analytics are not stored as a synchronously updated counter on `short_urls`. Events are published separately (best-effort).
