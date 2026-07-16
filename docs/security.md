# Security

## Implemented controls

| Control | Implementation |
|---|---|
| Destination scheme allowlist | HTTP/HTTPS only via `DestinationUrlValidator` |
| Structural URL validation | URI parse, host required, max length, no control characters |
| No destination fetch | Validation does not open network connections (SSRF avoided for MVP) |
| Parameterized persistence | Spring Data JPA / JDBC parameters |
| Uniqueness race handling | DB primary key is final uniqueness guarantee; bounded retry for generated codes |
| Soft-disable | Status transition without hard delete |
| Privacy-safe logs | Logs short-code length/status/error codes; not full destinations, secrets, or raw bodies |
| Secrets via env | Database/Redis credentials from environment variables; `.env` gitignored |
| Stable error responses | No stack traces, SQL, or internal class names in API errors |
| Request IDs | `X-Request-Id` / MDC for correlation |

## Deferred controls (not implemented)

- Authentication / authorization (especially for disable/management)
- Rate limiting and abuse throttling
- Malware / phishing / reputation scanning
- Domain blocklists
- Audit logging and data retention policies
- CAPTCHA / bot detection
- mTLS / private networking assumptions

Do not treat the public creation or disable APIs as production-ready without authentication and rate limits.

## SSRF considerations

Current validation is structural only. The application never fetches the submitted destination URL.

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

1. Unauthenticated create/disable in MVP demo configuration
2. No rate limiting
3. No malware/phishing scanning
4. Cache poisoning is mitigated by treating PostgreSQL as authority and invalidating on disable
