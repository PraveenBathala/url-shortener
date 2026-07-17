# Security

## Implemented controls

| Control | Implementation |
|---|---|
| Destination scheme allowlist | HTTP/HTTPS only via `DestinationUrlValidator` |
| Structural URL validation | URI parse, host required, max length, no control characters |
| No destination fetch | Validation does not open network connections (SSRF avoided for MVP) |
| Agentic safety pre-check | `UrlSafetyAgent` multi-tool loop; blocks HIGH risk (metadata/private hosts) |
| API key authentication | `X-API-Key` required for create, bulk, disable, analytics |
| Rate limiting | Per-client fixed window on create/bulk/disable (`app.security.rate-limit`) |
| Audit logging | Privacy-safe `AuditLogger` for create/disable/bulk |
| Parameterized persistence | Spring Data JPA / JDBC parameters |
| Uniqueness race handling | DB primary key is final uniqueness guarantee; bounded retry for generated codes |
| Soft-disable | Status transition without hard delete |
| Privacy-safe logs | Logs short-code length/status/error codes; not full destinations, secrets, or raw bodies |
| Structured JSON logs | `logback-spring.xml` + Logstash encoder for ELK/Loki |
| Secrets via env | Database/Redis/API key from environment variables; `.env` gitignored |
| Stable error responses | No stack traces, SQL, or internal class names in API errors |
| Request IDs | `X-Request-Id` / MDC for correlation |

## Deferred controls (not implemented)

- OAuth/OIDC / fine-grained RBAC
- Distributed rate limiting (Redis-backed) for multi-instance deployments
- Malware / phishing / remote reputation scanning
- Domain blocklists beyond heuristic host tools
- Data retention / GDPR erasure workflows
- CAPTCHA / bot detection
- mTLS / private networking assumptions

## Authentication

Management APIs require header:

```http
X-API-Key: <APP_API_KEY>
```

Protected: `POST /api/v1/urls`, `POST /api/v1/urls/bulk`, `DELETE /api/v1/urls/{code}`, `GET /api/v1/analytics/{code}`.

Public: redirects `GET /{shortCode}`, health, OpenAPI/Swagger UI.

Set a strong `APP_API_KEY` before any shared deployment. Default `dev-api-key-change-me` is for local demos only.

## SSRF considerations

Current validation and agent tools are structural/heuristic only. The application never fetches the submitted destination URL.

If reputation scanning is added later, isolate it behind a restricted component with:

- private-address / loopback / link-local / cloud-metadata blocking
- DNS revalidation
- redirect limits
- strict timeouts and response-size limits
- controlled egress

## Actuator exposure

Exposed locally: `health`, `info`, `prometheus`, `metrics`.

Production should restrict actuator endpoints (network policy, auth, or reduced exposure). `show-details` is `when_authorized`.

## Alias enumeration

Generated Base62 codes reduce casual enumeration versus sequential IDs, but are not a secrecy boundary. Rate limiting and monitoring remain required before abuse-sensitive deployments.

## Dependency / data risks

- Keep dependencies updated via CI
- PostgreSQL remains authoritative; Redis compromise must not rewrite destinations without DB access
- Analytics events intentionally omit raw IP and full destination URL

## Known risks

1. Shared API key is not per-user identity (upgrade to OIDC for multi-tenant brokerage contexts)
2. In-memory rate limiter is per JVM instance
3. No remote malware/phishing scanning yet
4. Cache poisoning is mitigated by treating PostgreSQL as authority and invalidating on disable
