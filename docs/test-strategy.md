# Test Strategy

## Layers

| Layer | Tools | Coverage |
|---|---|---|
| Unit | JUnit 5, Mockito | Generator, validators, services, cache adapter, analytics publisher, reliability, bulk, agentic safety |
| Controller | MockMvc `@WebMvcTest` | Status codes, Location header, error schema (security autoconfig excluded) |
| Persistence | `@DataJpaTest` + PostgreSQL Testcontainers | Flyway schema, persistence, uniqueness constraint |
| Integration | `@SpringBootTest` + MockMvc + PostgreSQL/Redis Testcontainers | Create → redirect → analytics → disable; auth rejection; bulk partial success; unknown/expired/disabled |

## Commands

Windows (local Docker Desktop):

```powershell
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"
.\mvnw.cmd clean test
.\mvnw.cmd clean verify
```

## Reliability cases

- Redis miss/failure → PostgreSQL fallback
- Analytics publisher exception → redirect still succeeds
- Collision retries bounded by configuration
- Disable invalidates cache and returns 410 on subsequent redirect
- Missing API key → `401 UNAUTHORIZED` on management APIs
- Agentic HIGH risk → create rejected with `URL_SAFETY_REJECTED`

## Notes

- Integration tests use real PostgreSQL/Redis via Testcontainers (not H2)
- Docker Engine 29 requires `src/test/resources/docker-java.properties` with `api.version=1.44`
- Flow IT sets `app.security.api-key=test-api-key` and disables rate limiting for determinism
- Never disable failing tests to force a green build
