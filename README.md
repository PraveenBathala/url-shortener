# URL Shortener

Production-oriented URL shortener for interview evaluation. PostgreSQL is the source of truth; Redis is a cache for redirects; analytics are best-effort and never block redirects.

## Architecture summary

- **Create:** validate destination → generate Base62 short code → insert into PostgreSQL (DB uniqueness + bounded retry) → `201`
- **Redirect:** Redis cache-aside → PostgreSQL fallback → `302` / `404` / `410` / `400` → async analytics publish
- **Disable:** soft-disable mapping, invalidate cache, `204` (auth required before production exposure)

Details: `docs/architecture.md`

## Technology choices

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Build | Maven Wrapper |
| Database | PostgreSQL + Flyway |
| Cache | Redis (cache-aside) |
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
| `APP_SHORT_CODE_LENGTH` | Generated code length | `7` |
| `APP_ANALYTICS_PUBLISH_TIMEOUT_MS` | Analytics timeout | `50` |

Never commit a real `.env` file.

## Local startup

```powershell
docker compose up -d
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

### Create

```http
POST /api/v1/urls
Content-Type: application/json

{
  "destinationUrl": "https://example.com/products/123",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

Returns `201` with `shortCode`, `shortUrl`, `destinationUrl`, timestamps, and `status`.

### Redirect

```http
GET /{shortCode}
```

- `302` + `Location` when active and unexpired
- `404` unknown
- `410` expired or disabled
- `400` malformed short code

### Disable

```http
DELETE /api/v1/urls/{shortCode}
```

Returns `204` on success, `404` if missing. **Authentication is required before production exposure.**

## Health and metrics

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

Restrict actuator endpoints in production.

## Security summary

HTTP/HTTPS-only structural validation, no destination fetch, parameterized persistence, privacy-safe logging, env-based secrets. Auth, rate limiting, and malware scanning are deferred. See `docs/security.md`.

## Known limitations / future improvements

- No authentication/authorization yet
- No rate limiting
- In-process analytics sink (replaceable interface)
- Custom aliases deferred
- Single-region MVP

## AI-assisted development

Cursor was used as an accelerator under `instructions.md`. Decisions and rejections are recorded in `docs/ai-usage-log.md`. The human developer owns review, Git, and submission.

## JAR build and run

```powershell
.\mvnw.cmd clean verify
java -jar target\url-shortener-0.0.1-SNAPSHOT.jar
```

Submission JAR: `target/url-shortener-0.0.1-SNAPSHOT.jar`

## Documentation index

| Doc | Purpose |
|---|---|
| `docs/requirements.md` | Requirements |
| `docs/assumptions.md` | Assumptions |
| `docs/architecture.md` | Design |
| `docs/data-model.md` | Schema |
| `docs/tradeoffs.md` | Tradeoffs |
| `docs/ai-usage-log.md` | AI usage |
| `docs/security.md` | Security |
| `docs/operations.md` | Ops |
| `docs/test-strategy.md` | Testing |
| `docs/interview-notes.md` | Interview prep |
| `docs/rubric-evidence.md` | Rubric mapping |
