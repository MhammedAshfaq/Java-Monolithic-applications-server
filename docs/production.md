# Production deployment

This document covers production-grade configuration and deployment for the Spring Monolith Template.

## Profiles

| Profile | Use case | Dev tools |
|---------|----------|-----------|
| `dev` | Local development, CI (optional) | **Enabled** — dashboard at `/` and `/dev` |
| `prod` | Production | **Disabled** — `/dev` returns 404 |

**Critical**: Never run production with the `dev` profile. The dev dashboard and `/dev/*` endpoints (DB health, Redis health) must not be exposed in production.

## Activating the production profile

- **Command line**: `java -jar app.jar --spring.profiles.active=prod`
- **Environment**: `SPRING_PROFILES_ACTIVE=prod`
- **System property**: `-Dspring.profiles.active=prod`

## Production configuration (`application-prod.yaml`)

The production profile:

- Disables DevTools restart.
- Exposes only the **health** actuator endpoint (no info, metrics, env in prod by default).
- Sets `show-sql: false` for JPA (override in your main config if needed).

Dev dashboard and all `/dev/*` endpoints are **not** registered because `DevDashboardController` is `@Profile("dev")`.

## Security

- **Dev routes**: `/dev` and `/dev/**` are permitted in `SecurityConfig`. In production they 404 because the controller is not loaded.
- **Actuator**: In prod, only `/actuator/health` is exposed by default. Restrict further or move to a management port if required.
- **Sensitive data**: Use environment variables or a secret manager for DB password, Redis password, and any API keys. Do not commit production secrets.

## Checklist

- [ ] Set `SPRING_PROFILES_ACTIVE=prod` (or equivalent) in production.
- [ ] Ensure `dev` profile is never active in production.
- [ ] Configure datasource and Redis (if used) via env or external config.
- [ ] Verify `/dev` and `/dev/*` return 404 in production.
- [ ] Expose only the actuator endpoints you need (e.g. health) and restrict access if required.
- [ ] Run Flyway migrations (enabled by default on startup) or manage them in your deployment pipeline.

## Further reading

- [Developer guide](developer-guide.md) — Local setup and running with `dev`
- [Dev tools](dev-tools.md) — What is disabled in production and why
