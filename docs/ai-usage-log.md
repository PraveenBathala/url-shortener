# AI Usage Log

This log records real Cursor-assisted decisions for the URL shortener assignment.
Only interactions that actually occurred are listed.

AI was used as an accelerator. Human ownership remains with the developer for
engineering decisions, validation, commits, and submission.

---

## Task or decision

Date: 2026-07-16

### Goal

Assess an empty repository against `instructions.md` and establish planning
documentation before writing application code.

### Context provided to Cursor

- Controlling instruction file `@instructions.md`
- Directive to work through all phases automatically
- Explicit ban on Git write operations
- Requirement for honest evidence and a tested executable JAR

### Prompt summary

Follow `instructions.md`, begin with repository assessment and planning, then
continue phase by phase with tests, documentation, and no Git writes.

### Cursor recommendation

Treat the repository as greenfield (only `instructions.md` present), create
Phase 1 planning docs (`requirements`, `assumptions`, `architecture`,
`tradeoffs`, `ai-usage-log`, `rubric-evidence`), then scaffold Spring Boot / Java 21.

### My evaluation

Agreed with greenfield assessment. Documentation-first before APIs matches the
phase order. Tooling check showed Java/Maven/Docker missing from PATH; installing
JDK 21 is required before tests can run.

### What I accepted

- Phase ordering and documentation set from instructions
- Preferred stack: Java 21, Spring Boot, PostgreSQL, Flyway, Redis, Testcontainers
- PostgreSQL as authority; Redis as cache only
- No Kafka / K8s for MVP

### What I changed or rejected

- Rejected generating the entire application in one uncontrolled pass
- Rejected any Git initialization/commit by Cursor (not a git repo yet; human owns Git)
- Deferred claiming any test/build success until commands actually execute

### Reason

Instructions require sequential phases, honest evidence, and human-owned Git.

### Validation performed

- Listed repository contents: only `instructions.md`
- Confirmed not a Git repository (`git status` failed with exit 128)
- Checked `java`, `mvn`, `docker` availability: not on PATH at assessment time
- Started Eclipse Temurin JDK 21 install via winget

### Related files and tests

- `instructions.md`
- `docs/requirements.md`
- `docs/assumptions.md`
- `docs/architecture.md`
- `docs/tradeoffs.md`
- `docs/ai-usage-log.md`
- `docs/rubric-evidence.md`

---

## Task or decision

Date: 2026-07-16

### Goal

Unblock Phase 2 Testcontainers against Docker Engine 29 on Windows.

### Context provided to Cursor

Phase 2 startup test skipped/failed with `BadRequestException (Status 400)` from Testcontainers while `docker run hello-world` succeeded.

### Prompt summary

Implicit from sequential phase execution after foundation scaffolding.

### Cursor recommendation

Initially used Testcontainers 1.20.6 with default Docker API negotiation.

### My evaluation

Root cause was Docker 29 minimum API 1.44 vs docker-java default 1.32, plus Windows named pipe selection (`dockerDesktopLinuxEngine`).

### What I accepted

- Upgrade Testcontainers to 1.21.4
- Pin `api.version=1.44` in `src/test/resources/docker-java.properties`
- Use `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` for local Windows runs

### What I changed or rejected

- Rejected hardcoding `docker.host` npipe into committed `testcontainers.properties` (breaks Linux CI)
- Rejected claiming skipped tests as full Phase 2 validation

### Reason

Portable CI + honest local evidence.

### Validation performed

`.\mvnw.cmd clean test` → 2 tests, 0 failures, 0 skipped.

### Related files and tests

- `pom.xml`
- `src/test/resources/docker-java.properties`
- `UrlShortenerApplicationTests`
- `AppPropertiesDefaultsTest`

---

## Task or decision

Date: 2026-07-16

### Goal

Decide whether to implement custom aliases in Phase 10.

### Context provided to Cursor

Phase 10 instruction: review existing API; defer if excessive scope.

### Prompt summary

Sequential phase execution after create/redirect APIs were stable.

### Cursor recommendation

Could implement alias validation, reserved words, 409 conflicts, and concurrency tests.

### My evaluation

Adds abuse surface and API complexity without improving core rubric evidence for create/redirect/cache/analytics.

### What I accepted

Defer custom aliases for MVP; document deferred rules in `docs/tradeoffs.md`.

### What I changed or rejected

Rejected implementing custom aliases in this pass.

### Reason

Prefer complete tested core over feature sprawl.

### Validation performed

Tradeoff documentation updated; generated-code path remains covered by tests.

### Related files and tests

- `docs/tradeoffs.md`
- `UrlCreationServiceTest`
- `UrlShortenerFlowIT`

---

## Task or decision

Date: 2026-07-16

### Goal

Make Redis cache-aside actually populate in integration tests.

### Context provided to Cursor

Flow IT logged `cachePut status=failure` while redirects still succeeded via PostgreSQL.

### Prompt summary

Implicit from Phase 11 validation.

### Cursor recommendation

`RedisTemplate<String,Object>` + `GenericJackson2JsonRedisSerializer`.

### My evaluation

Records are final; default typing/`GenericJackson2JsonRedisSerializer` struggled. Failures were correctly swallowed for fallback, but hid a broken cache write path.

### What I accepted

Keep fail-open cache behavior.

### What I changed or rejected

Rejected opaque Object Redis values; switched to `StringRedisTemplate` + explicit Jackson JSON for `CachedShortUrl`.

### Reason

Deterministic serialization and clearer failure modes.

### Validation performed

`UrlShortenerFlowIT` logged `cacheResult=miss` then `cacheResult=hit`. Full suite: 39 tests green.

### Related files and tests

- `RedisShortUrlCache.java`
- `RedisShortUrlCacheTest.java`
- `UrlShortenerFlowIT.java`

---

## Task or decision

Date: 2026-07-17

### Goal

Close assessment gaps: OpenAPI, auth, analytics HTTP API, bulk create, JSON logging, NFRs, and agentic artifacts.

### Context provided to Cursor

Assessment screenshots showing Strong Pass (~4.35) with missing OpenAPI/agentic artifacts; risks for unauthenticated create/disable, missing analytics endpoint, bulk gap, unstructured logs, thin AI usage log, and unquantified NFRs. Directive to build in `url-shortner -Agentic` and use an agentic framework where applicable.

### Prompt summary

Build the solution in the specified path, fix identified issues, cover scenarios, apply agentic framework as applicable.

### Cursor recommendation

Could add Spring AI / external LLM agent, OAuth2 resource server, Bucket4j, and Kafka analytics.

### My evaluation

External LLM would break hermetic tests and add secrets. OAuth is heavier than needed for an interview MVP. Prefer API key + deterministic tool-using agent + SpringDoc + persisted analytics counters.

### What I accepted

- Spring Security filter for `X-API-Key` on management APIs
- springdoc OpenAPI/Swagger
- `GET /api/v1/analytics/{code}` with Flyway-backed counters
- Bulk create with per-item partial success
- `logback-spring.xml` + Logstash JSON encoder
- In-memory rate limit + audit logger
- Quantified NFR targets in requirements

### What I changed or rejected

- Rejected remote LLM planner for MVP agent (use deterministic `UrlSafetyAgent` tool loop)
- Rejected OAuth/OIDC for this pass (document upgrade path)
- Rejected DNS lookup in host risk tool (literal IP / hostname heuristics only)

### Reason

Keep tests offline, preserve SSRF posture, close rubric gaps without infrastructure sprawl.

### Validation performed

Full Maven test suite executed after implementation (see latest `docs/rubric-evidence.md` result).

### Related files and tests

- `docs/agentic.md`, `UrlSafetyAgent*`, `SecurityConfig`, `AnalyticsController`
- `UrlShortenerFlowIT`, `UrlSafetyAgentTest`, `BulkUrlCreationServiceTest`

---

## Task or decision

Date: 2026-07-17

### Goal

Decide bulk create success semantics.

### Context provided to Cursor

Assessment noted bulk shortening missing and not documented in tradeoffs.

### Prompt summary

Implement bulk URL shortening as part of gap closure.

### Cursor recommendation

All-or-nothing transaction for the whole batch.

### My evaluation

Marketing/import batches often contain one bad URL; failing the entire batch is poor UX for operators.

### What I accepted

`POST /api/v1/urls/bulk` returning per-item `CREATED`/`FAILED` with HTTP 200.

### What I changed or rejected

Rejected all-or-nothing batch transaction.

### Reason

Partial success matches operational use; documented in `docs/tradeoffs.md`.

### Validation performed

`BulkUrlCreationServiceTest` + Flow IT bulk case.

### Related files and tests

- `BulkUrlCreationService.java`
- `BulkUrlCreationServiceTest.java`
- `UrlShortenerFlowIT.java`

---

## Task or decision

Date: 2026-07-17

### Goal

Finalize docs and submission evidence after gap closure.

### Context provided to Cursor

User asked to update docs and complete remaining changes before submit.

### Prompt summary

Update stale interview notes / test strategy, run `clean verify`, smoke-test the submission JAR.

### Cursor recommendation

Could skip JAR smoke and only refresh markdown.

### My evaluation

Submission criteria require an executable JAR with manual create/redirect verification; docs must match shipped auth/agentic/analytics behavior.

### What I accepted

- Refresh `docs/interview-notes.md` and `docs/test-strategy.md`
- Re-run `mvnw clean verify` (47 tests + failsafe)
- Smoke-test JAR against Compose Postgres/Redis including API key, analytics, OpenAPI

### What I changed or rejected

N/A for this pass beyond documentation accuracy.

### Reason

Honest evidence and interview answers must match the final code.

### Validation performed

`.\mvnw.cmd clean verify` → BUILD SUCCESS; JAR runtime checks recorded in `docs/rubric-evidence.md`.

### Related files and tests

- `docs/interview-notes.md`
- `docs/test-strategy.md`
- `docs/rubric-evidence.md`
- `target/url-shortener-0.0.1-SNAPSHOT.jar`
