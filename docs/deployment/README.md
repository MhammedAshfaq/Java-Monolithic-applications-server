# Deployment & Production

## Profiles

| Profile | Usage | Dev Tools | Actuator |
|---------|-------|-----------|----------|
| `dev` | Local development | Enabled | Full |
| `prod` | Production | Disabled | Health only |

---

## Activating Production Profile

### Option 1: Environment Variable (Recommended)

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

### Option 2: Command Line

```bash
java -jar app.jar --spring.profiles.active=prod
```

### Option 3: System Property

```bash
java -Dspring.profiles.active=prod -jar app.jar
```

### Docker

```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
```

---

## Production Configuration

### application-prod.yaml

```yaml
spring:
  config:
    activate:
      on-profile: prod

  devtools:
    restart:
      enabled: false

  jpa:
    show-sql: false

  data:
    redis:
      url: ${REDIS_URL:redis://localhost:6379}

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

---

## Environment Variables

### Required

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:postgresql://db:5432/app` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `app_user` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `secret` |

### Optional

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_URL` | Redis connection | `redis://localhost:6379` |
| `SERVER_PORT` | Application port | `8082` |

---

## Building for Production

### Build JAR

```bash
./mvnw clean package -DskipTests
```

Output: `target/spring-monolith-template-0.0.1-SNAPSHOT.jar`

### Run JAR

```bash
java -jar target/spring-monolith-template-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

---

## Docker Deployment

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/spring-monolith-template-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Build & Run

```bash
# Build
docker build -t spring-monolith-template .

# Run
docker run -d \
  -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db \
  -e SPRING_DATASOURCE_USERNAME=user \
  -e SPRING_DATASOURCE_PASSWORD=pass \
  spring-monolith-template
```

### Docker Compose

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8082:8082"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/app
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: secret
      REDIS_URL: redis://redis:6379
    depends_on:
      - db
      - redis

  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: app
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: secret
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass secret

volumes:
  postgres_data:
```

---

## Security Checklist

### Before Deployment

- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Verify `/dev` returns 404
- [ ] Use environment variables for secrets
- [ ] Enable HTTPS (via load balancer or reverse proxy)
- [ ] Configure proper CORS settings
- [ ] Set strong database password
- [ ] Set strong Redis password
- [ ] Review rate limit settings

### Verify Production Settings

```bash
# Check profile
curl http://localhost:8082/actuator/env | grep spring.profiles.active

# Verify dev endpoints disabled
curl http://localhost:8082/dev
# Should return 404

# Check only health is exposed
curl http://localhost:8082/actuator
# Should only show health endpoint
```

---

## Database Migrations

Flyway runs automatically on startup. For production:

### Option 1: Auto-migrate (Default)

Migrations run on application start.

### Option 2: Manual Migration

```yaml
# application-prod.yaml
spring:
  flyway:
    enabled: false
```

Run manually:
```bash
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://... -Dflyway.user=... -Dflyway.password=...
```

---

## Health Checks

### Kubernetes Probes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8082
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8082
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Enable Probes

```yaml
# application-prod.yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

---

## Logging

### Production Logging

```yaml
# application-prod.yaml
logging:
  level:
    root: WARN
    com.javainfraexample: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Log to File

```yaml
logging:
  file:
    name: /var/log/app/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
```

---

## Monitoring

### Expose Metrics (Optional)

```yaml
# application-prod.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

### Prometheus Integration

Add dependency:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Metrics available at: `/actuator/prometheus`

---

## Troubleshooting

### Application Won't Start

```bash
# Check logs
java -jar app.jar --debug

# Common issues:
# - Database connection failed
# - Port already in use
# - Missing environment variables
```

### Database Connection Issues

```bash
# Test connection
psql -h hostname -U username -d database

# Check environment
echo $SPRING_DATASOURCE_URL
```

### Redis Connection Issues

```bash
# Test connection
redis-cli -h hostname -a password ping
```
