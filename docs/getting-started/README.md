# Getting Started

Quick setup guide to get the application running locally.

## Prerequisites

| Requirement | Version | Required |
|-------------|---------|----------|
| Java | 21+ | Yes |
| Maven | 3.6+ (or use `./mvnw`) | Yes |
| PostgreSQL | 12+ | Yes |
| Redis | 6+ | Yes |
| RabbitMQ | 3.8+ | Optional |

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd spring-monolith-template
./mvnw clean install
```

### 2. Setup PostgreSQL

```bash
# Create database
psql -U postgres -c "CREATE DATABASE \"java-spring-mololithic\";"
```

Or update `application.yaml` with your database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_user
    password: your_password
```

### 3. Setup Redis

```bash
# Start Redis (if not running)
redis-server

# With password (recommended)
redis-cli CONFIG SET requirepass "yourStrongPassword123"
```

Update `application.yaml`:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: yourStrongPassword123
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

### 5. Access the Application

| URL | Description |
|-----|-------------|
| http://localhost:8082 | Dev Dashboard |
| http://localhost:8082/apidocs | Swagger API Docs |
| http://localhost:8082/api/health | Health Check |
| http://localhost:8082/actuator | Actuator Endpoints |

## Environment Variables

Instead of editing YAML files, use environment variables:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=secret

# Redis
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
export SPRING_DATA_REDIS_PASSWORD=secret

# Run
./mvnw spring-boot:run
```

## Common Commands

```bash
# Run application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Run JAR
java -jar target/spring-monolith-template-0.0.1-SNAPSHOT.jar

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## Troubleshooting

### Database Connection Failed

```
Connection refused to host: localhost; port: 5432
```

**Fix:** Ensure PostgreSQL is running:
```bash
# macOS
brew services start postgresql

# Linux
sudo systemctl start postgresql
```

### Redis Connection Failed

```
Unable to connect to Redis
```

**Fix:** Ensure Redis is running:
```bash
# macOS
brew services start redis

# Linux
sudo systemctl start redis
```

### Port Already in Use

```
Port 8082 was already in use
```

**Fix:** Kill the process or change port:
```bash
# Find process
lsof -i :8082

# Kill it
kill -9 <PID>

# Or change port in application.yaml
server:
  port: 8083
```

## Next Steps

- [Project Architecture](../architecture/README.md)
- [Database & Migrations](../database/README.md)
- [API Development](../api/README.md)
