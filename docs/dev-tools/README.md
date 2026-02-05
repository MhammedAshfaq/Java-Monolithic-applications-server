# Developer Tools

The Developer Dashboard provides quick access to all development tools in one place.

## Access

| URL | Description |
|-----|-------------|
| http://localhost:8082 | Dev Dashboard (root) |
| http://localhost:8082/dev | Dev Dashboard |
| http://localhost:8082/apidocs | Swagger UI |
| http://localhost:8082/actuator | Actuator Index |

**Note:** Dev tools are only available when running with the `dev` profile (default).

---

## Dashboard Features

### 1. Swagger API Docs

Interactive API documentation with "Try it out" functionality.

- **URL:** `/apidocs`
- **API Spec:** `/v1/api-docs`

### 2. Health Checks

| Endpoint | Description |
|----------|-------------|
| `/api/health` | Application health |
| `/dev/db-health` | Database connectivity |
| `/dev/redis-health` | Redis connectivity |

### 3. Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health status |
| `/actuator/info` | App info |
| `/actuator/metrics` | Metrics |
| `/actuator/env` | Environment |

### 4. External Tools

| Tool | Default URL |
|------|-------------|
| RabbitMQ Management | http://localhost:15672 |

---

## Configuration

### application-dev.yaml

```yaml
# Actuator endpoints exposed in dev
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
  endpoint:
    health:
      show-details: when-authorized

# Dev dashboard settings
app:
  dev-dashboard:
    rabbitmq-management-url: http://localhost:15672
```

---

## Dev-Only Endpoints

These endpoints are only registered when `dev` profile is active:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Dev dashboard HTML |
| GET | `/dev` | Dev dashboard HTML |
| GET | `/dev/db-health` | Test database connection |
| GET | `/dev/redis-health` | Test Redis connection |

### DB Health Response

```json
{
    "status": "up",
    "database": "connected",
    "message": "Database connection is healthy"
}
```

### Redis Health Response

```json
{
    "status": "up",
    "redis": "connected",
    "ping": "PONG"
}
```

---

## Production Behavior

In production (`prod` profile):

- `DevDashboardController` is **not loaded** (`@Profile("dev")`)
- `/dev` and `/dev/**` return **404 Not Found**
- Only `/actuator/health` is exposed
- Swagger UI is still available (can be disabled if needed)

---

## Disabling in Production

### Disable Dev Dashboard

Already disabled via `@Profile("dev")` on the controller.

### Disable Swagger in Production

Add to `application-prod.yaml`:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

### Restrict Actuator

```yaml
# application-prod.yaml
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

## Customizing the Dashboard

The dashboard is served from `DevDashboardController`. To customize:

1. Edit `api/dev/DevDashboardController.java`
2. Modify the HTML template in the `getDashboardHtml()` method
3. Add new health checks or links as needed

---

## Adding New Dev Tools

### Add a New Health Check

```java
// In DevDashboardController.java

@GetMapping("/dev/custom-health")
public ResponseEntity<Map<String, Object>> customHealth() {
    Map<String, Object> response = new HashMap<>();
    try {
        // Your health check logic
        response.put("status", "up");
        response.put("service", "connected");
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        response.put("status", "down");
        response.put("error", e.getMessage());
        return ResponseEntity.status(503).body(response);
    }
}
```

### Add External Tool Link

Update `application-dev.yaml`:

```yaml
app:
  dev-dashboard:
    rabbitmq-management-url: http://localhost:15672
    custom-tool-url: http://localhost:9000
```

Then reference in the dashboard HTML.
