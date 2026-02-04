# Documentation

Developer and operations documentation for the Spring Monolith Template.

## Contents

| Document | Description |
|----------|-------------|
| [Developer guide](developer-guide.md) | Local setup, running the app, testing, and development conventions |
| [Dev tools](dev-tools.md) | Developer dashboard, Swagger, RabbitMQ, Redis, DB health, and Actuator |
| [Production](production.md) | Production deployment, profiles, and security |

## Quick links

- **Run locally (dev)**: `./mvnw spring-boot:run` → [http://localhost:8082](http://localhost:8082)
- **Dev tools dashboard**: [http://localhost:8082/dev](http://localhost:8082/dev) (only when `dev` profile is active)
- **API docs (Swagger)**: [http://localhost:8082/apidocs](http://localhost:8082/apidocs)
- **Health**: [http://localhost:8082/health](http://localhost:8082/health)
