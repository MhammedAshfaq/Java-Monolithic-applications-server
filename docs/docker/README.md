# Docker Setup

This project supports two Docker configurations:

1. **Option A**: Infrastructure in Docker, Java app runs locally (recommended for development)
2. **Option B**: Everything in Docker (for testing production-like environment)

---

## Option A: Local Development (Recommended)

Java app runs locally for fast development with hot-reload.

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Your Machine                              │
│                                                                  │
│  ┌────────────────┐                                             │
│  │ Java App       │ ◄──── Runs locally (./mvnw spring-boot:run) │
│  │ (localhost:8082)│      Profile: dev                          │
│  └───────┬────────┘                                             │
│          │ connects via localhost                                │
│  ════════╪═══════════════════════════════════════════════════   │
│          │         Docker Compose Network                        │
│  ┌───────▼────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   PostgreSQL   │  │    Redis    │  │   pgAdmin   │          │
│  │  (port: 5432)  │  │ (port: 6379)│  │ (port: 5050)│          │
│  └────────────────┘  └─────────────┘  └─────────────┘          │
│                                        ┌─────────────┐          │
│                                        │Redis Cmdr   │          │
│                                        │ (port: 8081)│          │
│                                        └─────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### Quick Start

```bash
# 1. Start infrastructure only
docker-compose up -d postgres redis pgadmin redis-commander

# 2. Run Java app locally
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Configuration Used

- `application.yaml` (base config)
- `application-dev.yaml` (dev profile)
- Connects to: `localhost:5432`, `localhost:6379`

---

## Option B: Full Docker

Everything runs in Docker containers.

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Compose Network                        │
│                                                                  │
│  ┌────────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │   Spring App   │  │  PostgreSQL │  │    Redis    │          │
│  │  (port: 8082)  │──│  (postgres) │  │   (redis)   │          │
│  │ Profile:docker │  │             │  │             │          │
│  └────────────────┘  └─────────────┘  └─────────────┘          │
│                                                                  │
│  ┌─────────────┐  ┌─────────────────┐                          │
│  │   pgAdmin   │  │ Redis Commander │                          │
│  │ (port: 5050)│  │   (port: 8081)  │                          │
│  └─────────────┘  └─────────────────┘                          │
└─────────────────────────────────────────────────────────────────┘
```

### Quick Start

```bash
# Start everything (builds Java app)
docker-compose up -d

# Or build and start
docker-compose up -d --build
```

### Configuration Used

- `application.yaml` (base config)
- `application-docker.yaml` (docker profile)
- `application-dev.yaml` (dev profile)
- Profile: `docker,dev`
- Connects to: `postgres:5432`, `redis:6379` (container names)

### Rebuild After Code Changes

```bash
docker-compose up -d --build app
```

---

## Services

| Service | Port | URL | Credentials |
|---------|------|-----|-------------|
| Spring App | 8082 | http://localhost:8082 | - |
| PostgreSQL | 5432 | `localhost:5432` | `postgres` / `postgres` |
| pgAdmin | 5050 | http://localhost:5050 | `admin@admin.com` / `admin` |
| Redis | 6379 | `localhost:6379` | Password: `yourStrongPassword123` |
| Redis Commander | 8081 | http://localhost:8081 | `admin` / `admin` |

## Access Points

- **Application**: http://localhost:8082
- **Swagger UI**: http://localhost:8082/apidocs
- **Dev Dashboard**: http://localhost:8082/dev
- **pgAdmin**: http://localhost:5050
- **Redis Commander**: http://localhost:8081

## Commands

### Start Services

```bash
# Start all services
docker-compose up --build

# Start specific service
docker-compose up -d postgres redis

# Start with logs
docker-compose up
```

### Stop Services

```bash
# Stop all services (keeps data)
docker-compose stop

# Stop and remove containers (keeps volumes/data)
docker-compose down

# Stop and remove everything including data
docker-compose down -v
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f postgres
docker-compose logs -f redis
```

### Restart Services

```bash
# Restart all
docker-compose restart

# Restart specific service
docker-compose restart postgres
```

## Connecting to Services

### PostgreSQL

**From Java Application** (`application.yaml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/java-spring-mololithic
    username: postgres
    password: postgres
```

**From CLI**:
```bash
# Using docker exec
docker exec -it spring-postgres psql -U postgres -d java-spring-mololithic

# Using local psql client
psql -h localhost -p 5432 -U postgres -d java-spring-mololithic
```

### Redis

**From Java Application** (`application-dev.yaml`):
```yaml
spring:
  data:
    redis:
      url: redis://:yourStrongPassword123@localhost:6379
```

**From CLI**:
```bash
# Using docker exec
docker exec -it spring-redis redis-cli -a yourStrongPassword123

# Using local redis-cli
redis-cli -h localhost -p 6379 -a yourStrongPassword123
```

## pgAdmin Setup

1. Open http://localhost:5050
2. Login: `admin@admin.com` / `admin`
3. Add New Server:
   - **Name**: `spring-postgres`
   - **Host**: `postgres` (or `host.docker.internal` if not on same network)
   - **Port**: `5432`
   - **Username**: `postgres`
   - **Password**: `postgres`

> **Note**: When connecting from pgAdmin (inside Docker) to PostgreSQL (inside Docker), use the service name `postgres` as the host, not `localhost`.

## Redis Commander Usage

1. Open http://localhost:8081
2. Login: `admin` / `admin`
3. Browse keys, view values, run commands

**Features**:
- View all keys with prefixes (e.g., `user:*`, `rate_limit:*`)
- Inspect JSON values
- Set/Delete keys
- Monitor Redis commands

## Data Persistence

Data is stored in Docker volumes and persists across restarts:

```bash
# List volumes
docker volume ls

# Volumes created:
# - spring-monolith-template_postgres_data
# - spring-monolith-template_pgadmin_data
# - spring-monolith-template_redis_data
```

### Reset Data

```bash
# Remove all data (fresh start)
docker-compose down -v

# Remove specific volume
docker volume rm spring-monolith-template_postgres_data
```

## Troubleshooting

### Port Already in Use

```bash
# Check what's using the port
lsof -i :5432
lsof -i :6379

# Kill process or change port in docker-compose.yml
```

### Connection Refused

1. Check if containers are running:
   ```bash
   docker-compose ps
   ```

2. Check container logs:
   ```bash
   docker-compose logs postgres
   docker-compose logs redis
   ```

3. Ensure you're using `localhost` (not container name) from Java app

### Reset Everything

```bash
# Nuclear option - removes all containers, volumes, networks
docker-compose down -v --remove-orphans
docker-compose up -d
```

## Environment Variables

You can override settings using environment variables:

```bash
# Custom PostgreSQL password
POSTGRES_PASSWORD=mysecretpassword docker-compose up -d

# Or create a .env file
echo "POSTGRES_PASSWORD=mysecretpassword" > .env
docker-compose up -d
```

## Production Considerations

This setup is for **development only**. For production:

- [ ] Use external managed databases (RDS, Cloud SQL, etc.)
- [ ] Use Redis managed service (ElastiCache, Redis Cloud, etc.)
- [ ] Don't expose pgAdmin/Redis Commander
- [ ] Use strong, unique passwords
- [ ] Enable SSL/TLS connections
- [ ] Set up proper backups
- [ ] Use container orchestration (Kubernetes, ECS, etc.)
