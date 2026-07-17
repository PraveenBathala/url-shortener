# Rubric Evidence

Target overall score: at least **4.5 / 5**.

| Category | Weight | Target | Evidence |
|---|---:|---:|---|
| Problem Understanding and Reasoning | 15% | 4.5–5 | `docs/requirements.md`, `docs/assumptions.md`, `docs/tradeoffs.md` |
| Software Design and Architecture | 20% | 4.5–5 | `docs/architecture.md`, `docs/data-model.md`, layered packages under `src/main/java` |
| AI-Assisted Development | 20% | 5 | `docs/ai-usage-log.md` (real interactions only) |
| Code Quality | 15% | 4.5 | Constructor injection, focused services/controllers, immutable DTOs/records |
| Testing and Reliability | 10% | 4.5 | 47 automated tests incl. agentic/bulk/auth coverage + Testcontainers (when Docker available) |
| Security and Production Readiness | 10% | 4–4.5 | API key auth, rate limit, audit log, agentic HIGH-risk block, `docs/security.md` |
| Observability | 5% | 4 | Actuator, Micrometer, JSON `logback-spring.xml`, OpenAPI, `docs/operations.md` |
| Communication and Ownership | 5% | 5 | README + interview notes + explicit AI ownership |

## Phase log

### Phase 1 — Planning

Docs created; repository was greenfield (`instructions.md` only). Not a Git repo. JDK/Docker installed to unblock builds.

### Phase 2 — Foundation

Spring Boot 3.4.5, Java 21, Maven Wrapper, Compose, env config, startup test.

Command: `.\mvnw.cmd clean test`  
Result: `BUILD SUCCESS` (after Testcontainers Docker API pin).

### Phases 3–9 — Domain through Testcontainers flow

Entity/migration/repository, Base62 generator, destination validation, create/redirect APIs, global errors, PostgreSQL IT.

Command: `.\mvnw.cmd clean test`  
Result: `Tests run: 27, Failures: 0, Errors: 0, Skipped: 0` (pre-Redis).

### Phase 10 — Custom aliases

**Deferred.** Documented in `docs/tradeoffs.md`.

### Phases 11–13 — Redis, disable, analytics

Cache-aside with PostgreSQL fallback, soft-disable + cache eviction, best-effort analytics publisher with timeout.

### Phases 14–16 — Security, observability, reliability

Security/ops/test docs; metrics component; reliability unit tests.

### Phases 17–19 — Docker/CI/JAR

Dockerfile, GitHub Actions, `clean verify`, JAR runtime checks (see below when executed).

## Gap-closure pass (2026-07-17)

Added: OpenAPI/Swagger, API key auth, rate limiting, audit logging, analytics HTTP API, bulk create, JSON logging, quantified NFRs, agentic `UrlSafetyAgent` (`docs/agentic.md`).

## Latest full suite result

```text
Command: $env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"; .\mvnw.cmd clean test
Result: Tests run: 47, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

Includes Testcontainers coverage: `UrlShortenerFlowIT` (create/redirect/analytics/disable/auth/bulk), `UrlShortenerApplicationTests`, `ShortUrlRepositoryTest`.

## Final JAR validation (Phase 19 — revalidated 2026-07-17)

**Submission JAR:** `target/url-shortener-0.0.1-SNAPSHOT.jar` (≈74 MB)

**Build command:**

```text
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
.\mvnw.cmd clean verify
```

**Result:** Surefire `Tests run: 47, Failures: 0, Errors: 0, Skipped: 0`; Failsafe Flow IT `Tests run: 4` — `BUILD SUCCESS`

**Runtime checks against** `java -jar target/url-shortener-0.0.1-SNAPSHOT.jar` with Compose PostgreSQL/Redis (`APP_API_KEY=dev-api-key-change-me`):

| Check | Actual |
|---|---|
| App starts | `Started UrlShortenerApplication` |
| Health | `200` `{"status":"UP"}` |
| Create without API key | `401` |
| Create with API key | `201` with shortCode/shortUrl |
| Redirect | `302` with `Location` to destination |
| Analytics | `200` with `redirectCount` ≥ 1 |
| Invalid URL | `400` |
| Unknown code | `404` |
| Disable + redirect | `204` then `410` |
| OpenAPI | `200` OpenAPI 3.1.0 at `/v3/api-docs` |
| Metrics | `200` for `urlshortener.redirects` |
| Secrets in JAR | No `.env` / credentials entries in JAR listing |
| Metadata | `META-INF/MANIFEST.MF`, `META-INF/build-info.properties`, `BOOT-INF/classes/git.properties` present |
