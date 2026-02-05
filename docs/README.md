# Documentation

Developer and operations documentation for the Spring Monolith Template.

## Contents

| Document | Description |
|----------|-------------|
| [Developer Guide](developer-guide.md) | Local setup, running the app, testing |
| [Dev Tools](dev-tools.md) | Developer dashboard, Swagger, Actuator |
| [Rate Limiting](rate-limit/README.md) | IP-based rate limiting with Redis |
| [Production](production.md) | Production deployment and security |

## Quick Links

- **Run locally (dev)**: `./mvnw spring-boot:run` → [http://localhost:8082](http://localhost:8082)
- **Dev dashboard**: [http://localhost:8082/dev](http://localhost:8082/dev) (dev profile only)
- **API docs (Swagger)**: [http://localhost:8082/apidocs](http://localhost:8082/apidocs)
- **Health check**: [http://localhost:8082/api/health](http://localhost:8082/api/health)

## Project Structure

```
src/main/java/.../
├── api/                    # REST Controllers
│   ├── auth/
│   │   ├── AuthController.java
│   │   └── dto/            # Request/Response DTOs
│   ├── dev/
│   └── health/
├── services/               # Business Logic
│   ├── auth/
│   └── health/
├── common/                 # Shared Components
│   ├── dto/                # Common DTOs (ApiResponseDto)
│   └── ratelimit/          # Rate limiting
└── config/                 # Configuration
    ├── redis/
    ├── security/
    └── swagger/
```
