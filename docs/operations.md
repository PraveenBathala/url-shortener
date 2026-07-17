# Operations

## Startup

```powershell
docker compose up -d
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/urlshortener"
$env:DATABASE_USERNAME="urlshortener"
$env:DATABASE_PASSWORD="urlshortener"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:APP_BASE_URL="http://localhost:8080"
$env:APP_API_KEY="dev-api-key-change-me"
.\mvnw.cmd spring-boot:run
```

Or run the packaged JAR:

```powershell
java -jar target\url-shortener-0.0.1-SNAPSHOT.jar
```

## Shutdown

1. Stop the application process (Ctrl+C or SIGTERM)
2. `docker compose down` (add `-v` only if wiping local data intentionally)

## Health checks

- `GET /actuator/health`
- Compose services include PostgreSQL and Redis healthchecks

## Metrics

- `GET /actuator/metrics`
- `GET /actuator/prometheus`

Custom meters include:

- `urlshortener.urls.created`
- `urlshortener.redirects`
- `urlshortener.redirects.unknown|expired|disabled`
- `urlshortener.shortcode.collisions`
- `urlshortener.cache.hits|misses|failures`
- `urlshortener.analytics.publish.failures`
- creation/redirect latency timers

## Logging

Default console output is **JSON** via `logback-spring.xml` (Logstash encoder) for ELK/Loki.

For human-readable local logs:

```powershell
$env:SPRING_PROFILES_ACTIVE="local-text"
```

## OpenAPI

- Swagger UI: `/swagger-ui.html`
- Spec: `/v3/api-docs`

## Failure behavior

| Dependency | User-visible behavior |
|---|---|
| Redis unavailable | Redirects continue via PostgreSQL; cache failure metrics increase |
| Analytics unavailable/timeout | Redirects continue; analytics failure metrics increase; events may be lost |
| PostgreSQL unavailable | Create/redirect fail with controlled 5xx; no silent corruption |
| Collision retries exhausted | Controlled `SHORT_CODE_GENERATION_FAILED` response |
| Missing/invalid API key | `401 UNAUTHORIZED` on management APIs |
| Rate limit exceeded | `429 RATE_LIMIT_EXCEEDED` on create/bulk/disable |
| Agentic HIGH risk | `400 URL_SAFETY_REJECTED` |

## Suggested alerts

- Elevated `urlshortener.cache.failures`
- Elevated `urlshortener.analytics.publish.failures`
- Elevated 5xx rate
- PostgreSQL connection pool exhaustion
- Redirect latency above agreed threshold (target, not measured here)

## SLO targets (targets only — not measured results)

- 99.99% redirect availability when PostgreSQL is healthy
- 99% cached redirect latency under an agreed threshold
- Bounded creation error rate

## Recovery notes

1. Restore PostgreSQL first (source of truth)
2. Redis can be flushed/rebuilt; cache will repopulate on miss
3. Lost analytics events are acceptable under MVP loss policy
4. After disable operations, verify cache eviction if stale redirects are observed
