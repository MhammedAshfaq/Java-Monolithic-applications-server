# Dev tools

The **Developer Dashboard** is a single page that links to Swagger, RabbitMQ, Redis and database health, and Actuator. It is **only available when the `dev` profile is active** and is not loaded in production.

## Access

- **URL**: [http://localhost:8082](http://localhost:8082) or [http://localhost:8082/dev](http://localhost:8082/dev)
- **When**: Only if the application is started with the **dev** profile (default for local run).
- **Production**: The dev dashboard and all `/dev/*` endpoints are **not** registered when the `prod` profile is active; requests to `/dev` return 404.

## Dashboard links

| Link | Description |
|------|-------------|
| **Swagger Dashboard** | API docs and “Try it out” at `/apidocs` |
| **RabbitMQ Dashboard** | External link to RabbitMQ Management UI (default: `http://localhost:15672`) |
| **Health** | Application health at `/health` |
| **Database health** | Dev-only endpoint that tests DB connectivity: `GET /dev/db-health` |
| **Redis health** | Dev-only endpoint that tests Redis (PING): `GET /dev/redis-health` |
| **Actuator** | Spring Boot Actuator index at `/actuator` (health, info, metrics, env in dev) |

## Dev-only endpoints

These are registered only when `dev` profile is active.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Serves the dev tools HTML page (same as `/dev`) |
| `/dev` | GET | Serves the dev tools HTML page |
| `/dev/db-health` | GET | Checks database connectivity (e.g. `Connection.isValid`). Returns JSON with `status`, `database`, and optional `message`/`error`. |
| `/dev/redis-health` | GET | Checks Redis with PING. Returns JSON with `status`, `redis`, and optional `ping`/`error`. If Redis is not configured, returns `"redis": "not configured"`. |

All dev endpoints return JSON except `/` and `/dev`, which return HTML with `Cache-Control: no-store` so the page is not cached.

## Configuration

Dev-specific settings live in `application-dev.yaml` and (for some defaults) in `application.yaml`.

| Property | Default | Description |
|----------|---------|-------------|
| `app.dev-dashboard.rabbitmq-management-url` | `http://localhost:15672` | Link for the “RabbitMQ Dashboard” button |
| `spring.data.redis.url` (in dev) | — | Redis URL including password, e.g. `redis://:password@localhost:6379` |
| `management.endpoints.web.exposure.include` (dev) | `health,info,metrics,env` | Actuator endpoints exposed in dev |

## Production behaviour

- **Profile**: In production, use `spring.profiles.active=prod` (or equivalent). Do **not** use the `dev` profile in production.
- **Dev controller**: `DevDashboardController` is annotated with `@Profile("dev")`, so it is not created when the active profile is not `dev`. No dev dashboard and no `/dev/*` endpoints are exposed.
- **Security**: In the main `SecurityConfig`, `/dev` and `/dev/**` are permitted. In prod they simply 404 because the controller does not exist.
- **Caching**: The dev dashboard response is sent with `Cache-Control: no-store` so it is not cached by browsers or proxies.

## Optional Redis

If Redis is not configured or the Redis connection factory is not available, the app still starts. The **Redis health** card and `GET /dev/redis-health` will return a 503 body with `"redis": "not configured"` when Redis is not present.

See [Developer guide](developer-guide.md) for local Redis setup and [Production](production.md) for production configuration.
