# Rubric Evidence

Target overall score: at least **4.5 / 5**.

| Category | Weight | Target | Evidence |
|---|---:|---:|---|
| Problem Understanding and Reasoning | 15% | 4.5–5 | `docs/requirements.md`, `docs/assumptions.md`, `docs/tradeoffs.md` |
| Software Design and Architecture | 20% | 4.5–5 | `docs/architecture.md`, `docs/data-model.md`, layered packages under `src/main/java` |
| AI-Assisted Development | 20% | 5 | `docs/ai-usage-log.md` (real interactions only) |
| Code Quality | 15% | 4.5 | Constructor injection, focused services/controllers, immutable DTOs/records |
| Testing and Reliability | 10% | 4.5 | 39 automated tests incl. Testcontainers + reliability behaviors |
| Security and Production Readiness | 10% | 4–4.5 | `docs/security.md`, destination validator, privacy-safe logs, deferred controls documented |
| Observability | 5% | 4 | Actuator, Micrometer counters/timers, structured log pattern, `docs/operations.md` |
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

## Latest full suite result

```text
Command: $env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"; .\mvnw.cmd clean test
Result: Tests run: 39, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

Observed in Flow IT: `cacheResult=miss` then `cacheResult=hit` on second redirect.

## Final JAR validation (Phase 19)

**Submission JAR:** `target/url-shortener-0.0.1-SNAPSHOT.jar` (≈68 MB)

**Build command:**

```text
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
.\mvnw.cmd clean verify
```

**Result:** `Tests run: 39` in surefire + failsafe Flow IT rerun — `BUILD SUCCESS`

**Runtime checks against** `java -jar target/url-shortener-0.0.1-SNAPSHOT.jar` with Compose PostgreSQL/Redis:

| Check | Actual |
|---|---|
| App starts | `Started UrlShortenerApplication` |
| Health | `200` `{"status":"UP"}` |
| Create | `201` with shortCode/shortUrl |
| Redirect | `302` `Location: https://example.com/products/123` |
| Invalid URL | `400` `INVALID_DESTINATION_URL` |
| Unknown code | `404` `SHORT_CODE_NOT_FOUND` |
| Disable + redirect | `204` then `410` `SHORT_CODE_DISABLED` |
| Metrics | `200` for `urlshortener.redirects` |
| Redis stopped fallback | create `201`, redirect `302` via PostgreSQL |
| Secrets in JAR | No `.env` / password files packaged |
| Metadata | `META-INF/MANIFEST.MF`, `META-INF/build-info.properties`, `BOOT-INF/classes/git.properties` present (limited git fields because repo has no `.git`) |
