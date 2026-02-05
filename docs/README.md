# Technical Documentation

Complete technical documentation for the Spring Monolith Template.

## Quick Start

```bash
# Clone & build
git clone <repository-url>
cd spring-monolith-template
./mvnw clean install

# Run (requires PostgreSQL & Redis)
./mvnw spring-boot:run

# Access
open http://localhost:8082       # Dev Dashboard
open http://localhost:8082/apidocs  # Swagger
```

---

## Documentation Index

| Section | Description |
|---------|-------------|
| [Getting Started](getting-started/README.md) | Prerequisites, setup, running the app |
| [Architecture](architecture/README.md) | Project structure, layers, conventions |
| [Database & Migrations](database/README.md) | Entities, repositories, Flyway migrations |
| [API Development](api/README.md) | Controllers, DTOs, validation, Swagger |
| [Rate Limiting](rate-limit/README.md) | IP-based rate limiting with Redis |
| [Dev Tools](dev-tools/README.md) | Developer dashboard, health checks |
| [Deployment](deployment/README.md) | Production config, Docker, security |

---

## Common Commands

### Running

```bash
# Development (default)
./mvnw spring-boot:run

# Production mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Run tests
./mvnw test

# Build JAR
./mvnw clean package
```

### Database Migrations

```bash
# Create new migration
./scripts/create-migration.sh <action_name>

# Examples
./scripts/create-migration.sh create_products_table
./scripts/create-migration.sh add_phone_to_users
./scripts/create-migration.sh add_index_on_orders_status
```

Migrations run automatically on startup.

### Docker

```bash
# Build image
docker build -t spring-monolith-template .

# Run with Docker Compose
docker-compose up -d
```

---

## Project Structure

```
spring-monolith-template/
├── src/main/java/.../
│   ├── api/                # REST Controllers + DTOs
│   ├── services/           # Business Logic
│   ├── domain/             # Entities + Enums
│   ├── repository/         # Data Access (JPA)
│   ├── common/             # Shared (DTOs, Rate Limit)
│   └── config/             # Configuration
│
├── src/main/resources/
│   ├── application.yaml
│   ├── application-dev.yaml
│   ├── application-prod.yaml
│   └── db/migration/       # Flyway Migrations
│
├── scripts/
│   └── create-migration.sh
│
└── docs/                   # This documentation
```

---

## Key URLs (Development)

| URL | Description |
|-----|-------------|
| http://localhost:8082 | Dev Dashboard |
| http://localhost:8082/apidocs | Swagger UI |
| http://localhost:8082/api/health | Health Check |
| http://localhost:8082/actuator | Actuator |
| http://localhost:8082/dev/db-health | Database Health |
| http://localhost:8082/dev/redis-health | Redis Health |

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 4.0 |
| Language | Java 21 |
| Database | PostgreSQL |
| Cache | Redis |
| ORM | JPA / Hibernate |
| Connection Pool | HikariCP |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI |
| Build | Maven |

---

## Configuration Files

| File | Purpose |
|------|---------|
| `application.yaml` | Common settings |
| `application-dev.yaml` | Development overrides |
| `application-prod.yaml` | Production overrides |
| `pom.xml` | Maven dependencies |

---

## Environment Variables

### Required for Production

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=secret
REDIS_URL=redis://:password@host:6379
```

---

## Support

For issues or questions:

1. Check relevant documentation section
2. Search existing issues
3. Create a new issue with:
   - Steps to reproduce
   - Expected vs actual behavior
   - Logs/error messages
