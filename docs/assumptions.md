# Assumptions

These assumptions guide design and scope for the MVP.

| Assumption | Implication |
|---|---|
| Redirect traffic is significantly higher than URL-creation traffic | Optimize the redirect path (cache-aside, thin controllers, async analytics) |
| PostgreSQL is the source of truth | All authoritative reads/writes go through PostgreSQL; Redis is disposable |
| Redis is only a cache | Redis failure must fall back to PostgreSQL; never treat Redis as authoritative |
| Only HTTP and HTTPS destination URLs are accepted | Reject `javascript:`, `file:`, `data:`, `ftp:`, and other schemes |
| Analytics may be eventually consistent | Redirect publishing is best-effort; lost events are acceptable when the publisher fails |
| Redirects must not fail solely because analytics is unavailable | Analytics errors are logged/metered but do not change redirect HTTP status |
| The MVP is single-region | No multi-region replication, global uniqueness coordination, or geo routing |
| Management APIs use a shared API key for the interview MVP | Create/bulk/disable/analytics require `X-API-Key`; redirects stay public |
| OAuth/OIDC is a later upgrade for multi-tenant brokerage identity | Documented in security tradeoffs; not required for single-tenant demo |
| Multi-region deployment and sharding are future improvements | Documented scaling path only |
| Advanced malware and phishing detection are future improvements | Validation is structural only; no remote fetch of destinations |
| Local performance testing does not represent production capacity | SLO targets are documented targets, not measured claims |

## Operational assumptions

- Docker is available for local PostgreSQL/Redis and Testcontainers.
- Java 21 and Maven Wrapper are used for build/test/package.
- Environment variables supply database/Redis credentials; no secrets are committed.
- Clock skew is negligible for expiration checks within a single-region deployment.
