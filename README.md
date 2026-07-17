# URL Shortener

Production-oriented URL shortener for interview evaluation. PostgreSQL is the source of truth; Redis is a cache for redirects; analytics are best-effort and never block redirects. An agentic URL-safety loop runs on create.

## Architecture summary

- **Create:** API key → validate destination → agentic safety tools → generate Base62 code → PostgreSQL insert → audit → `201`
- **Bulk create:** per-item create with partial success (`CREATED` / `FAILED`)
- **Redirect:** Redis cache-aside → PostgreSQL fallback → `302` / `404` / `410` / `400` → async analytics publish
- **Analytics:** `GET /api/v1/analytics/{code}` (API key) reads persisted counters
- **Disable:** API key → soft-disable → cache invalidate → audit → `204`

Details: `docs/architecture.md`, agent loop: `docs/agentic.md`

## Technology choices

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Build | Maven Wrapper |
| Database | PostgreSQL + Flyway |
| Cache | Redis (cache-aside) |
| Security | API key + rate limit |
| API docs | springdoc OpenAPI / Swagger UI |
| Logging | Logback JSON (Logstash encoder) |
| Agentic | Deterministic multi-tool `UrlSafetyAgent` |
| Tests | JUnit 5, Mockito, Testcontainers |
| Observability | Actuator, Micrometer/Prometheus |
| Local infra | Docker Compose |
| CI | GitHub Actions |

## Prerequisites

- Java 21
- Docker Desktop (Compose + Testcontainers)
- On Windows with Docker Engine 29, set:
  `$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"`

## Environment variables

See `.env.example`.

| Variable | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/urlshortener` |
| `DATABASE_USERNAME` | DB user | `urlshortener` |
| `DATABASE_PASSWORD` | DB password | `urlshortener` |
| `REDIS_HOST` / `REDIS_PORT` | Redis | `localhost` / `6379` |
| `APP_BASE_URL` | Public base for short URLs | `http://localhost:8080` |
| `APP_API_KEY` | Management API key | `dev-api-key-change-me` |
| `APP_SECURITY_REQUIRE_API_KEY` | Enforce API key | `true` |
| `APP_RATE_LIMIT_PER_MINUTE` | Write rate limit | `60` |
| `APP_AGENTIC_ENABLED` | Run safety agent on create | `true` |

Never commit a real `.env` file.

## Local startup

```powershell
docker compose up -d
$env:APP_API_KEY = "dev-api-key-change-me"
.\mvnw.cmd spring-boot:run
```

## Shutdown

Stop the app with Ctrl+C, then `docker compose down`.

## Tests

```powershell
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

## API examples

OpenAPI UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Create

```http
POST /api/v1/urls
Content-Type: application/json
X-API-Key: dev-api-key-change-me

{
  "destinationUrl": "https://example.com/products/123",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

Returns `201` with `shortCode`, `shortUrl`, `destinationUrl`, timestamps, and `status`.

### Bulk create

```http
POST /api/v1/urls/bulk
Content-Type: application/json
X-API-Key: dev-api-key-change-me

{
  "urls": [
    {"destinationUrl": "https://example.com/a"},
    {"destinationUrl": "https://example.com/b"}
  ]
}
```

### Redirect

```http
GET /{shortCode}
```

- `302` + `Location` when active and unexpired
- `404` unknown
- `410` expired or disabled
- `400` malformed short code

### Analytics

```http
GET /api/v1/analytics/{shortCode}
X-API-Key: dev-api-key-change-me
```

### Disable

```http
DELETE /api/v1/urls/{shortCode}
X-API-Key: dev-api-key-change-me
```

Returns `204` on success, `404` if missing.

## Health and metrics

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

Restrict actuator endpoints in production.

## Security summary

API key on management APIs, rate limiting on writes, agentic HIGH-risk URL blocking, HTTP/HTTPS structural validation, no destination fetch, parameterized persistence, privacy-safe JSON logging, env-based secrets. See `docs/security.md`.

## Known limitations / future improvements

- Shared API key (not OIDC / per-user RBAC)
- In-memory rate limiter (per instance)
- In-process analytics sink (replaceable; counters persisted)
- Custom aliases deferred
- Single-region MVP
- No remote malware reputation API

## AI-assisted development

Cursor was used as an accelerator under `instructions.md`. Decisions and rejections are recorded in `docs/ai-usage-log.md`. The human developer owns review, Git, and submission.

## JAR build and run

```powershell
.\mvnw.cmd clean verify
$env:APP_API_KEY = "dev-api-key-change-me"
java -jar target\url-shortener-0.0.1-SNAPSHOT.jar
```

Submission JAR: `target/url-shortener-0.0.1-SNAPSHOT.jar`

## Documentation index

| Doc | Purpose |
|---|---|
| `docs/requirements.md` | Requirements + quantified NFRs |
| `docs/assumptions.md` | Assumptions |
| `docs/architecture.md` | Design |
| `docs/data-model.md` | Schema |
| `docs/tradeoffs.md` | Tradeoffs |
| `docs/agentic.md` | Agentic safety loop |
| `docs/ai-usage-log.md` | AI usage |
| `docs/security.md` | Security |
| `docs/operations.md` | Ops |
| `docs/test-strategy.md` | Testing |
| `docs/interview-notes.md` | Interview prep |
| `docs/rubric-evidence.md` | Rubric mapping |
