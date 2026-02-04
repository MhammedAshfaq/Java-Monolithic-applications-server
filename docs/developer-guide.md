# Developer guide

This guide covers local setup, running the application, testing, and development conventions.

## Prerequisites

- **Java 21**
- **Maven 3.6+** (or use `./mvnw`)
- **PostgreSQL 12+**
- **Redis** (optional for dev; required if using Redis features)
- **RabbitMQ** (optional for dev; required if using AMQP)

## Local setup

### 1. Clone and build

```bash
git clone <repository-url>
cd spring-monolith-template
./mvnw clean install
```

### 2. Database

Create a PostgreSQL database and set credentials in `src/main/resources/application.yaml` (or use env vars):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/java-spring-mololithic
    username: postgres
    password: postgres
```

Flyway runs migrations on startup. Place scripts in `src/main/resources/db/migration/` with names like `V1__description.sql`.

### 3. Redis (optional)

Default: `localhost:6379`. With password, set in `application-dev.yaml`:

```yaml
spring:
  data:
    redis:
      url: redis://:yourPassword@localhost:6379
```

### 4. RabbitMQ (optional)

Default: `localhost:5672`. Management UI: [http://localhost:15672](http://localhost:15672).

## Running the application

### Development (default)

The default profile is `dev`, so the dev tools dashboard is available at the root and at `/dev`.

```bash
./mvnw spring-boot:run
```

- Application: [http://localhost:8082](http://localhost:8082)
- Dev tools: [http://localhost:8082/dev](http://localhost:8082/dev)
- Swagger: [http://localhost:8082/apidocs](http://localhost:8082/apidocs)

### With explicit profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production-like locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Dev tools and `/dev` are **not** available when the `prod` profile is active.

## Project structure

```
src/main/java/.../
├── api/                    # REST controllers and feature modules
│   ├── example/
│   └── health/
├── common/                 # Shared DTOs, exceptions, utilities
│   ├── dto/
│   ├── exception/
│   └── util/
├── config/                 # Configuration and security
│   ├── dev/                # Dev-only (dashboard, dev health endpoints)
│   ├── security/
│   └── swagger/
└── SpringMonolithTemplateApplication.java
```

- **API**: One package per feature (e.g. `api/health`, `api/example`). Controllers under `api/*` get the global `/api` prefix except paths listed in `WebMvcConfig` (e.g. `/health`).
- **API**: One package per feature (e.g. `api/health`, `api/example`). Controllers under `api/*` get the global `/api` prefix except paths listed in `WebMvcConfig` (e.g. `/health`).
- **Config**: `api.dev` (dev dashboard) is only loaded when the `dev` profile is active.

## Conventions

- **API responses**: Use `ApiResponseDto<T>` and `ResponseUtil` for consistent JSON shape.
- **Exceptions**: Use domain exceptions in `common.exception`; `GlobalExceptionHandler` maps them to HTTP status and `ErrorDetails`.
- **Health**: Application health at `/health`; DB and Redis health checks are available only in dev at `/dev/db-health` and `/dev/redis-health`.

## Testing

```bash
./mvnw test
```

See the main [README](../README.md) for more on testing and API documentation.

## Further reading

- [Dev tools](dev-tools.md) — Developer dashboard and all dev-only endpoints
- [Production](production.md) — Deployment and production configuration
