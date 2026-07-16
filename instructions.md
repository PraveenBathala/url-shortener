# Cursor Instructions — URL Shortener Assignment

## Objective

Build a production-oriented URL shortener that can reasonably score at least **4.5 out of 5** against the provided AI-Proficient Software Developer rubric.

Cursor may implement the project automatically in phases, but the human developer remains responsible for all engineering decisions, validation, and final submission.

The final deliverable is a runnable JAR. The repository should also contain enough documentation and tests to support the interview discussion.

---

# 1. Non-Negotiable Rules

## 1.1 Git safety

Cursor must never perform Git write operations.

Cursor may use read-only commands such as:

```text
git status
git diff
git diff --stat
git log
git show
git branch --show-current
git ls-files
```

Cursor must never run:

```text
git add
git commit
git commit --amend
git push
git pull
git merge
git rebase
git cherry-pick
git reset
git restore
git checkout
git switch
git stash
git clean
git tag
git rm
git mv
git update-index
git update-ref
```

Cursor must also never:

- modify anything under `.git`
- alter Git configuration
- create branches or tags
- stage files
- rewrite history
- change author or commit timestamps
- use `--no-verify`
- create scripts or workflows that auto-commit
- claim that a commit was made
- claim that human review occurred

The human developer performs all staging, commits, and pushes manually.

At the end of each phase, Cursor may suggest one commit message, but must not execute it.

---

## 1.2 Honesty and evidence

Cursor must not fabricate:

- test results
- performance results
- build results
- security findings
- AI interactions
- rejected AI suggestions
- development duration
- manual review
- commit activity
- production readiness
- deployment success

Only report what was actually executed or verified.

If a command cannot run, explain why and document the limitation.

---

## 1.3 Automatic coding behavior

Cursor should work automatically through the phases in this file.

For each phase:

1. Inspect the current repository.
2. Identify what already exists.
3. Plan the smallest coherent change.
4. Implement the change.
5. Run focused tests.
6. Fix failures.
7. Run the broader test suite.
8. Update relevant documentation.
9. Report results.
10. Continue to the next phase unless:
   - a blocking requirement is genuinely ambiguous
   - required infrastructure is unavailable
   - the decision would materially change the public API
   - the decision introduces a major security or data model change

Do not artificially delay work.

Do not generate the entire application in one uncontrolled pass.

---

## 1.4 Human ownership

The implementation and documentation must make it clear that AI was used as an accelerator, not as the source of truth.

Cursor should:

- explain major decisions
- identify risks
- identify assumptions
- highlight AI suggestions that required review
- add tests that validate generated code
- document real examples of accepted, changed, or rejected AI recommendations

Cursor must not make the work look falsely non-AI-generated.

The correct goal is transparent, disciplined, human-owned AI-assisted development.

---

# 2. Rubric Target

The assignment is evaluated across these categories:

| Category | Weight | Target |
|---|---:|---:|
| Problem Understanding and Reasoning | 15% | 4.5–5 |
| Software Design and Architecture | 20% | 4.5–5 |
| AI-Assisted Development | 20% | 5 |
| Code Quality | 15% | 4.5 |
| Testing and Reliability | 10% | 4.5 |
| Security and Production Readiness | 10% | 4–4.5 |
| Observability | 5% | 4 |
| Communication and Ownership | 5% | 5 |

Every implementation phase must add real evidence for one or more rubric categories.

---

# 3. Technology Stack

Use the existing project configuration when present.

Preferred stack:

- Java 21
- Spring Boot
- Maven Wrapper
- PostgreSQL
- Flyway
- Redis
- JUnit 5
- Mockito
- Testcontainers
- Spring Boot Actuator
- Micrometer
- Docker Compose
- GitHub Actions

Do not add the following unless the core application is complete and there is a clear reason:

- Kafka
- Kubernetes
- cloud-specific infrastructure
- database sharding
- multi-region deployment
- service mesh
- event streaming platforms
- complex authentication frameworks

The assignment should favor a complete, tested solution over unnecessary infrastructure.

---

# 4. Product Requirements

The system must support:

1. Creating a short URL from a destination URL
2. Returning HTTP 201 for successful creation
3. Resolving a short code
4. Returning HTTP 302 with a `Location` header
5. Validating destination URLs
6. Generating unique Base62-style short codes
7. Safely handling short-code collisions
8. Optional expiration dates
9. Disabled links
10. Consistent error responses
11. PostgreSQL persistence
12. Redis cache-aside behavior
13. Database fallback when Redis is unavailable
14. Asynchronous or replaceable analytics event publishing
15. Structured logs
16. Health endpoints
17. Metrics
18. Automated tests
19. Docker-based local execution
20. A final executable JAR

---

# 5. Assumptions

Document these in `docs/assumptions.md`:

- Redirect traffic is significantly higher than URL-creation traffic.
- PostgreSQL is the source of truth.
- Redis is only a cache.
- Only HTTP and HTTPS destination URLs are accepted.
- Analytics may be eventually consistent.
- Redirects must not fail solely because analytics is unavailable.
- The MVP is single-region.
- Authentication may be deferred for the public creation API.
- Management operations require authentication before real production exposure.
- Multi-region deployment and sharding are future improvements.
- Advanced malware and phishing detection are future improvements.
- Local performance testing does not represent production capacity.

---

# 6. Architecture Principles

## 6.1 Creation path

```text
Client
  |
Creation Controller
  |
Request Validation
  |
Short-Code Generator
  |
Creation Service
  |
PostgreSQL
  |
Optional cache population
```

## 6.2 Redirect path

```text
Client
  |
Redirect Controller
  |
Redirect Service
  |
Redis Cache
  |
PostgreSQL on cache miss
  |
302 response
```

## 6.3 Analytics path

```text
Redirect Service
  |
Best-effort Event Publisher
  |
Analytics Consumer or Event Sink
```

Redirect success must not depend on analytics availability.

## 6.4 Component boundaries

Use clear separation:

- controllers: HTTP concerns
- services: business logic
- repositories: persistence
- validators: input validation
- generators: short-code generation
- cache adapters: Redis behavior
- event publishers: analytics delivery
- exception handlers: API error mapping
- configuration: environment and infrastructure settings
- metrics components: telemetry

Do not place business logic in controllers.

---

# 7. Coding Standards

Use:

- constructor injection
- focused methods
- descriptive names
- immutable DTOs where practical
- UTC timestamps
- `Clock` injection for time-based logic where practical
- configuration properties
- environment variables
- minimal dependencies
- centralized error handling

Avoid:

- field injection
- mutable static state
- unnecessary inheritance
- generic utility classes
- premature abstraction
- comments that repeat the code
- large controllers
- oversized services
- hardcoded credentials
- hardcoded environment URLs
- placeholder methods
- unimplemented TODOs
- fake production behavior
- unused dependencies
- dead code

Comments should explain reasoning, not syntax.

---

# 8. Security Standards

## 8.1 URL validation

Accept only:

```text
http
https
```

Require:

- non-blank input
- valid URI structure
- supported scheme
- valid host
- reasonable maximum length
- no control characters

Reject:

```text
javascript:
file:
data:
ftp:
```

Do not connect to the submitted URL during normal validation.

Do not fetch submitted URLs from the main application.

If reputation scanning is later added, isolate it behind a restricted component with:

- private-address blocking
- loopback blocking
- cloud metadata blocking
- DNS revalidation
- redirect limits
- strict timeouts
- response-size limits
- controlled network egress

## 8.2 Persistence

Use Spring Data repositories or parameterized SQL.

The database uniqueness constraint must be the final uniqueness guarantee.

Do not rely only on:

```java
repository.existsById(code)
```

That check is subject to a race condition.

## 8.3 Logging

Do not log:

- full destination URLs
- query parameters that may contain tokens
- authorization headers
- passwords
- API keys
- database credentials
- raw request bodies
- personal information

## 8.4 Secrets

Read secrets from environment variables.

Do not commit:

- real `.env` files
- passwords
- private keys
- certificates
- access tokens

## 8.5 Production gaps

Document deferred controls:

- authentication
- authorization
- rate limiting
- malware detection
- phishing detection
- abuse reporting
- data retention
- audit logging
- domain blocklists

Do not claim that deferred controls are implemented.

---

# 9. Database Design

Use Flyway.

The main mapping table should contain fields equivalent to:

```text
short_code
destination_url
status
created_at
updated_at
expires_at
version
```

Requirements:

- `short_code` is the primary key or uniquely constrained
- `destination_url` is required
- `status` is required
- timestamps are required
- expiration is optional
- optimistic versioning may be used
- analytics data is stored separately
- no synchronously updated click counter in the mapping row

Add indexes only for actual query or cleanup needs.

Document every non-trivial index.

---

# 10. Short-Code Generation

Create:

```java
ShortCodeGenerator
```

Default implementation:

- `SecureRandom`
- Base62 character set

```text
0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
```

- configurable length
- default length: 7
- startup validation for invalid configuration

Collision behavior:

1. Generate code.
2. Attempt insert.
3. Allow the database constraint to detect collision.
4. Retry only for system-generated codes.
5. Use a configurable maximum retry count.
6. Return a controlled error if exhausted.

Do not claim that random generation guarantees uniqueness.

---

# 11. HTTP API

## 11.1 Create URL

```http
POST /api/v1/urls
```

Example request:

```json
{
  "destinationUrl": "https://example.com/products/123",
  "expiresAt": "2027-01-01T00:00:00Z"
}
```

Example response:

```json
{
  "shortCode": "aB7xK9P",
  "shortUrl": "http://localhost:8080/aB7xK9P",
  "destinationUrl": "https://example.com/products/123",
  "createdAt": "2026-07-16T20:00:00Z",
  "expiresAt": "2027-01-01T00:00:00Z",
  "status": "ACTIVE"
}
```

Use:

```text
201 Created
```

Validation:

- destination is required
- only HTTP/HTTPS
- expiration must be in the future
- custom alias rules when implemented

## 11.2 Redirect

```http
GET /{shortCode}
```

Behavior:

```text
302 Found       active and unexpired
404 Not Found   unknown code
410 Gone        expired
410 Gone        disabled
400 Bad Request malformed short code
```

Use HTTP 302 by default.

## 11.3 Disable

Preferred endpoint:

```http
DELETE /api/v1/urls/{shortCode}
```

Behavior:

- soft-disable
- update modification timestamp
- invalidate Redis
- return 204
- return 404 when not found

Document authentication as required before public production exposure.

---

# 12. Redis Behavior

Add Redis only after PostgreSQL creation and redirect flows are stable.

Use cache-aside behavior.

Requirements:

- PostgreSQL remains authoritative
- cache active mappings
- do not cache beyond URL expiration
- optionally use short negative caching
- invalidate on status or destination changes
- fall back to PostgreSQL during Redis failure
- record cache hit, miss, and failure metrics
- avoid distributed locks unless tests prove the need
- use TTL jitter only when justified

Redis failure must not make valid links unavailable while PostgreSQL is healthy.

---

# 13. Analytics Behavior

Do not synchronously update click counts during redirect.

Use a replaceable interface such as:

```java
RedirectEventPublisher
```

Requirements:

- strict publish timeout
- redirect continues if publishing fails
- failure metric
- minimal event fields
- no raw IP unless explicitly required
- clear retry or event-loss behavior
- duplicate-event strategy documented

Kafka is optional and should not be added unless justified.

An in-process adapter or lightweight event implementation is acceptable if the interface and failure behavior are clean.

---

# 14. Error Handling

Use centralized exception handling.

Return a stable error structure containing:

```text
timestamp
status
errorCode
message
path
requestId
```

Do not expose:

- stack traces
- SQL details
- database names
- class names
- internal exception messages
- credentials

Create controlled exceptions for:

- invalid destination
- invalid short code
- unknown short code
- expired short code
- disabled short code
- duplicate custom alias
- generation retry exhaustion

---

# 15. Testing Strategy

## 15.1 Unit tests

Cover:

- Base62 generator
- valid and invalid URLs
- expiration
- disabled state
- collision retry
- retry exhaustion
- exception mapping
- cache behavior
- analytics failure behavior

## 15.2 Controller tests

Cover:

- status codes
- response body
- validation errors
- error schema
- redirect `Location` header

## 15.3 Integration tests

Use PostgreSQL Testcontainers.

Do not rely only on H2.

Cover:

1. Flyway migration
2. Create URL through HTTP
3. Verify persistence
4. Resolve short code through HTTP
5. Verify 302 and Location
6. Unknown code
7. Expired code
8. Disabled code
9. Collision handling where practical

## 15.4 Redis tests

Cover:

- cache hit
- cache miss
- cache invalidation
- expiration-aware TTL
- negative caching
- Redis unavailable
- PostgreSQL fallback

## 15.5 Reliability tests

Cover:

- Redis failure
- analytics failure
- database failure
- retry exhaustion
- concurrent alias requests
- bounded timeout behavior

## 15.6 Test execution

For every phase:

1. Run the focused test.
2. Fix the smallest root cause.
3. Rerun the failed test.
4. Run the broader suite.
5. Report the exact command.
6. Report the actual result.

Never disable failing tests to make the build green.

---

# 16. Observability

## 16.1 Health

Use Spring Boot Actuator.

Expose only appropriate health information.

Document production restrictions for sensitive actuator endpoints.

## 16.2 Metrics

Add:

- URL creation count
- redirect count
- creation latency
- redirect latency
- cache hits
- cache misses
- cache failures
- unknown-code count
- expired-code count
- disabled-code count
- collision retry count
- analytics publication failures

## 16.3 Structured logs

Include useful fields:

```text
timestamp
service
environment
requestId
traceId
operation
status
latency
cacheResult
errorCode
```

Do not log full destinations.

## 16.4 Targets

Targets may be documented.

Do not present targets as measured results.

Example targets:

```text
99.99% redirect availability
99% cached redirect latency under an agreed threshold
bounded creation error rate
```

---

# 17. Required Documentation

Maintain:

```text
README.md
docs/requirements.md
docs/assumptions.md
docs/architecture.md
docs/tradeoffs.md
docs/ai-usage-log.md
docs/test-strategy.md
docs/security.md
docs/operations.md
docs/interview-notes.md
docs/rubric-evidence.md
```

## README must include

- project purpose
- architecture summary
- technology choices
- prerequisites
- setup
- environment variables
- startup
- shutdown
- tests
- API examples
- health endpoint
- metrics endpoint
- security summary
- known limitations
- future improvements
- AI-assisted development summary
- JAR build and run instructions

## Architecture documentation

Explain:

- creation path
- redirect path
- cache behavior
- analytics behavior
- database choice
- scaling path
- failure handling

## Tradeoffs documentation

Cover:

- PostgreSQL versus distributed key-value databases
- random codes versus sequence-derived codes
- 302 versus 301/308
- cache-aside versus alternatives
- synchronous versus asynchronous analytics
- Testcontainers versus H2
- custom aliases versus generated codes
- strong versus eventual consistency

## Security documentation

Include:

- implemented controls
- deferred controls
- SSRF considerations
- secrets handling
- logging rules
- production gaps
- known risks

## Operations documentation

Include:

- startup
- shutdown
- health checks
- metrics
- common failures
- Redis failure behavior
- database failure behavior
- analytics failure behavior
- suggested alerts
- SLO targets
- recovery notes

---

# 18. AI Usage Log

Maintain:

```text
docs/ai-usage-log.md
```

Each real entry should contain:

```markdown
## Task or decision

Date:

### Goal

### Context provided to Cursor

### Prompt summary

### Cursor recommendation

### My evaluation

### What I accepted

### What I changed or rejected

### Reason

### Validation performed

### Related files and tests
```

Good real examples include:

- Cursor suggested `existsById()` before insert; changed to database-constraint collision handling.
- Cursor suggested synchronous click-count updates; changed to asynchronous event publishing.
- Cursor suggested H2-only integration tests; changed to PostgreSQL Testcontainers.
- Cursor logged full URLs; changed to privacy-safe logging.
- Cursor accepted unsupported URL schemes; tests exposed the defect and validation was corrected.
- Cursor returned HTTP 200 for creation; changed to HTTP 201.

Only record examples that actually occurred.

---

# 19. Implementation Phases

Cursor should complete these phases automatically and sequentially.

---

## Phase 1 — Repository assessment and planning

Tasks:

- inspect repository
- identify current files and dependencies
- identify missing pieces
- create or update:
  - `docs/requirements.md`
  - `docs/assumptions.md`
  - `docs/architecture.md`
  - `docs/tradeoffs.md`
  - `docs/ai-usage-log.md`
  - `docs/rubric-evidence.md`

Do not write application code before understanding the repository.

Rubric evidence:

- problem reasoning
- architecture
- communication
- AI ownership

---

## Phase 2 — Project foundation

Tasks:

- configure Java 21
- configure Spring Boot
- configure Maven Wrapper
- add PostgreSQL
- add Flyway
- add Docker Compose
- add test configuration
- add startup test
- add environment-variable configuration

Do not implement the APIs yet.

Run:

```text
./mvnw clean test
```

or on Windows:

```text
mvnw.cmd clean test
```

---

## Phase 3 — Domain and persistence

Tasks:

- create `ShortUrl`
- create status enum
- create repository
- create Flyway migration
- add database constraints
- add focused persistence tests
- document data model

Do not add Redis yet.

---

## Phase 4 — Short-code generator

Tasks:

- create generator interface
- implement SecureRandom Base62 generator
- configure default length
- validate configuration
- add tests

Document that uniqueness is enforced by the database constraint.

---

## Phase 5 — URL validation

Tasks:

- create destination validator
- accept only HTTP/HTTPS
- reject malformed values
- reject missing hosts
- reject control characters
- reject oversized input
- add tests
- document SSRF reasoning

Do not make network requests.

---

## Phase 6 — Creation API

Tasks:

- implement `POST /api/v1/urls`
- return 201
- keep controller thin
- validate expiration
- implement collision retry
- use database constraint
- add service tests
- add controller tests
- add integration tests

Do not add Redis yet.

---

## Phase 7 — Redirect API

Tasks:

- implement `GET /{shortCode}`
- return 302
- set `Location`
- return 404 for unknown
- return 410 for expired
- return 410 for disabled
- return 400 for malformed
- add service and HTTP tests

---

## Phase 8 — Central error handling

Tasks:

- add `@RestControllerAdvice`
- create stable error schema
- add request ID
- prevent internal detail leakage
- add tests for each public error

---

## Phase 9 — PostgreSQL Testcontainers

Tasks:

- add PostgreSQL Testcontainers
- verify Flyway
- test full create-and-redirect flow
- test unknown
- test expired
- test disabled
- test persistence behavior
- document actual test command and result

---

## Phase 10 — Optional custom alias and idempotency

Before implementing, review the existing API.

If the feature adds excessive scope, document it as deferred.

If implemented:

- validate alias characters
- enforce min/max length
- reserve system words
- return 409 on conflict
- do not retry custom aliases
- add concurrency tests
- define idempotency behavior clearly

---

## Phase 11 — Redis

Tasks:

- add cache-aside behavior
- preserve PostgreSQL authority
- add expiration-aware TTL
- add invalidation
- add negative caching if useful
- add database fallback
- add cache metrics
- add tests

Do not change public API behavior.

---

## Phase 12 — Disable operation

Tasks:

- implement soft-disable
- update timestamp
- invalidate cache
- return 204
- return 404 if missing
- document authentication requirement
- add tests

---

## Phase 13 — Analytics event publisher

Tasks:

- create replaceable event publisher
- use strict timeout
- do not block redirects
- add failure metrics
- minimize event data
- add publisher tests
- document loss and retry behavior

Do not add Kafka unless justified.

---

## Phase 14 — Security review

Review:

- URL validation
- SSRF
- SQL injection
- secret management
- logging
- rate limiting
- authentication
- authorization
- alias enumeration
- dependency risks
- data retention
- actuator exposure

Implement high-value controls within scope.

Update:

```text
docs/security.md
```

---

## Phase 15 — Observability

Tasks:

- structured logging
- request ID
- Actuator
- Micrometer
- creation metrics
- redirect metrics
- cache metrics
- failure metrics
- operations documentation

Do not claim unmeasured latency.

---

## Phase 16 — Reliability testing

Tasks:

- Redis failure
- analytics failure
- collision exhaustion
- database failure
- timeout behavior
- concurrent alias requests
- bounded retries

Document expected user-visible behavior.

---

## Phase 17 — Docker and CI

Tasks:

- multi-stage Dockerfile
- non-root user where practical
- external configuration
- health checks
- Docker Compose
- GitHub Actions
- compile
- unit tests
- integration tests
- package JAR

Do not skip tests.

---

## Phase 18 — Final code review

Review for:

- duplicate code
- large classes
- large methods
- unnecessary abstractions
- dead code
- unused dependencies
- leaked secrets
- sensitive logging
- race conditions
- missing timeouts
- unbounded retries
- placeholder behavior
- inaccurate documentation
- weak assertions

Apply the smallest corrections.

Run the full suite.

---

## Phase 19 — Final JAR validation

Build:

```text
mvnw.cmd clean verify
```

or:

```text
./mvnw clean verify
```

Identify the exact JAR intended for submission.

Run the exact JAR:

```text
java -jar target/<final-jar-name>.jar
```

Verify:

1. application starts
2. health endpoint works
3. create API works
4. redirect works
5. invalid URL is rejected
6. unknown code returns 404
7. expired or disabled link returns 410
8. Redis failure falls back to PostgreSQL
9. metrics are available
10. no secrets are packaged

Inspect the JAR contents for:

```text
git.properties
build-info.properties
MANIFEST.MF
```

Document whether Git metadata is embedded.

Do not remove normal build metadata unless there is a reason.

---

# 20. Cursor Response Format

Before each phase:

```text
Phase:
Rubric categories addressed:
Current repository state:
Assumptions:
Affected components:
Risks:
Implementation plan:
Validation plan:
```

After each phase:

```text
Phase completed:
Files changed:
Important decisions:
Tests executed:
Actual results:
Rubric evidence added:
Remaining risks:
Human review items:
Suggested commit message:
```

Always end with:

```text
No files were staged, committed, pushed, tagged, or added to Git history.
```

---

# 21. Final Review Criteria

Do not declare completion until:

- the JAR builds
- the exact submitted JAR runs
- create flow is manually verified
- redirect flow is manually verified
- the full automated test suite passes
- Flyway works on a clean database
- Redis fallback is tested
- analytics failure does not break redirects
- health and metrics are documented
- security limitations are documented
- README commands are accurate
- AI usage log contains only real interactions
- known limitations are listed
- interview notes are prepared
- rubric evidence points to actual files and tests
- no Git write operation was performed by Cursor

---

# 22. Interview Preparation

Prepare concise answers for:

- How did you decompose the problem?
- What assumptions did you make?
- Why PostgreSQL?
- Why Redis?
- Why is Redis not authoritative?
- Why HTTP 302?
- How are collisions handled?
- What happens when Redis fails?
- What happens when analytics fails?
- How is SSRF avoided?
- Why Testcontainers?
- What AI tools were used?
- How were prompts structured?
- What AI output was changed or rejected?
- How were hallucinations detected?
- What is not production-ready?
- What would you improve with more time?
- How would the design scale?

The final explanation should demonstrate full ownership.

---

# 23. First Command to Cursor

When this file is added to the project root, start Cursor Auto mode with:

```text
@instructions.md

Follow this file as the controlling instruction for the repository.

Work automatically through all implementation phases in order.

Do not perform any Git write operation.

Begin with repository assessment and planning. Then continue phase by phase.

For each phase:
- inspect existing work
- implement the smallest coherent change
- run focused tests
- fix failures
- run the broader test suite
- update documentation
- report actual results
- continue automatically unless a genuinely blocking architectural or security decision requires human input

Do not fabricate results.
Do not claim human review.
Do not stage, commit, push, tag, or modify Git history.

The final goal is a tested executable JAR and complete rubric evidence.
```
