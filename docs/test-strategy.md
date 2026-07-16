# Test Strategy

## Layers

| Layer | Tools | Coverage |
|---|---|---|
| Unit | JUnit 5, Mockito | Generator, validators, services, cache adapter, analytics publisher, reliability behaviors |
| Controller | MockMvc `@WebMvcTest` | Status codes, Location header, error schema |
| Persistence | `@DataJpaTest` + PostgreSQL Testcontainers | Flyway schema, persistence, uniqueness constraint |
| Integration | `@SpringBootTest` + MockMvc + PostgreSQL/Redis Testcontainers | Create → redirect → disable, unknown/expired/disabled |

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

## Notes

- Integration tests use real PostgreSQL via Testcontainers (not H2)
- Docker Engine 29 requires `src/test/resources/docker-java.properties` with `api.version=1.44`
- Never disable failing tests to force a green build
