# Monitoring (Prometheus + Grafana)

## Overview

| Component | Technology | Port | Purpose |
|-----------|------------|------|---------|
| Metrics Library | Micrometer | — | Collects metrics inside the app (JVM, HTTP, DB, cache) |
| Metrics Endpoint | Spring Actuator | 8082 | Exposes `/actuator/prometheus` for scraping |
| Metrics Storage | Prometheus | 9090 | Scrapes & stores time-series metrics every 15s |
| Visualization | Grafana | 3000 | Dashboards, graphs, alerts |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│                                                              │
│  ┌──────────┐   ┌───────────┐   ┌─────────────────────┐     │
│  │ Your Code│──▶│ Micrometer│──▶│ /actuator/prometheus │     │
│  │ (HTTP,   │   │ (counters,│   │ (text format, auto-  │     │
│  │  DB,     │   │  gauges,  │   │  generated metrics)  │     │
│  │  cache)  │   │  timers)  │   └──────────┬──────────┘     │
│  └──────────┘   └───────────┘              │                 │
└────────────────────────────────────────────┼─────────────────┘
                                             │ scrape every 15s
                                             ▼
                                    ┌─────────────────┐
                                    │   Prometheus     │
                                    │  :9090           │
                                    │  (time-series DB)│
                                    └────────┬────────┘
                                             │ query (PromQL)
                                             ▼
                                    ┌─────────────────┐
                                    │    Grafana       │
                                    │  :3000           │
                                    │  (dashboards)    │
                                    └─────────────────┘
```

---

## Quick Start

### 1. Start the monitoring stack

```bash
# Start Prometheus + Grafana (along with other services)
docker-compose up -d prometheus grafana

# Or start everything
docker-compose up -d
```

### 2. Access the UIs

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | `admin` / `admin` |
| Prometheus | http://localhost:9090 | No auth |
| Raw Metrics | http://localhost:8082/actuator/prometheus | No auth |

### 3. View the pre-built dashboard

1. Open **Grafana** → http://localhost:3000
2. Login with `admin` / `admin` (skip password change)
3. Go to **Dashboards** → **Spring Boot** folder → **Spring Boot Overview**

The dashboard is **auto-provisioned** — no manual setup needed.

---

## What Metrics Are Collected

Micrometer automatically collects metrics from Spring Boot. No code changes needed for most of these.

### HTTP Requests

| Metric | Description |
|--------|-------------|
| `http_server_requests_seconds_count` | Total request count (by URI, method, status) |
| `http_server_requests_seconds_sum` | Total request duration |
| `http_server_requests_seconds_bucket` | Histogram buckets for percentile calculation |

### JVM

| Metric | Description |
|--------|-------------|
| `jvm_memory_used_bytes` | Memory used (heap / non-heap, by pool) |
| `jvm_memory_committed_bytes` | Memory committed by JVM |
| `jvm_memory_max_bytes` | Max memory limit |
| `jvm_threads_live_threads` | Current live thread count |
| `jvm_threads_daemon_threads` | Daemon thread count |
| `jvm_threads_peak_threads` | Peak thread count |
| `jvm_gc_pause_seconds` | GC pause duration |
| `system_cpu_usage` | System CPU usage (0-1) |
| `process_cpu_usage` | JVM process CPU usage (0-1) |
| `process_uptime_seconds` | Application uptime |

### Database (HikariCP)

| Metric | Description |
|--------|-------------|
| `hikaricp_connections_active` | Active DB connections |
| `hikaricp_connections_idle` | Idle DB connections |
| `hikaricp_connections` | Total connections in pool |
| `hikaricp_connections_pending` | Threads waiting for a connection |
| `hikaricp_connections_acquire_seconds` | Time to acquire a connection |
| `hikaricp_connections_creation_seconds` | Time to create a new connection |

### Cache (Redis)

Two types of cache metrics are available:

**Spring Cache (`@Cacheable`)** — auto-instrumented by Micrometer:

| Metric | Description |
|--------|-------------|
| `cache_gets_total{result="hit"}` | Cache hits from `@Cacheable` |
| `cache_gets_total{result="miss"}` | Cache misses from `@Cacheable` |
| `cache_puts_total` | Cache puts from `@Cacheable` / `@CachePut` |

**RedisCacheService (manual ops)** — custom Micrometer counters:

| Metric | Description |
|--------|-------------|
| `redis_cache_hits_total` | `RedisCacheService.get()` returned a value |
| `redis_cache_misses_total` | `RedisCacheService.get()` returned null |
| `redis_cache_puts_total` | `RedisCacheService.set()` calls |
| `redis_cache_deletes_total` | `RedisCacheService.delete()` calls |

---

## Grafana Dashboard Panels

The **Spring Boot Overview** dashboard is auto-provisioned and contains:

### Row 1: Application Overview (stat panels)

| Panel | Metric | Description |
|-------|--------|-------------|
| Application Status | `up{job="spring-boot-app"}` | UP (green) / DOWN (red) |
| Uptime | `process_uptime_seconds` | How long the app has been running |
| Request Rate | `rate(http_server_requests_seconds_count[5m])` | Requests per second |
| Error Rate (5xx) | 5xx count / total count | Percentage of server errors |
| Avg Response Time | sum / count of request durations | Average latency |
| P95 Response Time | `histogram_quantile(0.95, ...)` | 95th percentile latency |

### Row 2: HTTP Requests (time-series graphs)

| Panel | Shows |
|-------|-------|
| Request Rate by URI | Requests/sec broken down by endpoint |
| Response Time Percentiles | P50, P95, P99 latency over time |
| Request Rate by Status Code | HTTP 200, 400, 401, 500, etc. |
| Response Time by URI | Average response time per endpoint |

### Row 3: JVM Memory

| Panel | Shows |
|-------|-------|
| JVM Heap Memory | Used / committed / max heap memory per pool |
| JVM Non-Heap Memory | Metaspace, code cache, etc. |

### Row 4: JVM Threads & CPU

| Panel | Shows |
|-------|-------|
| JVM Threads | Live / daemon / peak thread counts |
| GC Pause Duration | Garbage collection pause time by cause |
| System CPU Usage | System CPU vs. JVM process CPU |

### Row 5: Database (HikariCP)

| Panel | Shows |
|-------|-------|
| Connection Pool | Active / idle / total / pending connections |
| Connection Acquire Time | How long it takes to get a DB connection |

### Row 6: Cache (Redis)

| Panel | Shows |
|-------|-------|
| Cache Operations | Hits / misses / puts for both `@Cacheable` and manual Redis |
| Cache Hit Ratio | Hit rate as a percentage (green > 80%, red < 50%) |

---

## Configuration

### Spring Boot (application.yaml)

The base monitoring config applies to all profiles:

```yaml
management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 500ms, 1s, 5s
```

### Per-Profile Overrides

| Profile | Endpoints Exposed | Health Details |
|---------|-------------------|----------------|
| **dev** | health, info, prometheus, metrics, env, caches, scheduledtasks | Always shown |
| **prod** | health, info, prometheus | Only when authorized |

### Prometheus (monitoring/prometheus/prometheus.yml)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  scrape_timeout: 10s

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['spring-app:8082']  # Docker container name
```

### Grafana Provisioning

Auto-configured on startup — no manual setup:

```
monitoring/
├── prometheus/
│   └── prometheus.yml              # Prometheus scrape config
└── grafana/
    └── provisioning/
        ├── datasources/
        │   └── prometheus.yml      # Auto-connect Grafana → Prometheus
        └── dashboards/
            ├── dashboards.yml      # Auto-load dashboard JSON files
            └── json/
                └── spring-boot-overview.json  # Pre-built dashboard
```

---

## Useful PromQL Queries

Use these in Prometheus (http://localhost:9090) or Grafana Explore.

### Request Rate

```promql
# Total requests per second
sum(rate(http_server_requests_seconds_count[5m]))

# Requests per second by endpoint
sum(rate(http_server_requests_seconds_count[5m])) by (uri)

# Requests per second by status code
sum(rate(http_server_requests_seconds_count[5m])) by (status)
```

### Latency

```promql
# Average response time
sum(rate(http_server_requests_seconds_sum[5m])) / sum(rate(http_server_requests_seconds_count[5m]))

# P95 response time
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# P99 response time
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))

# P95 by endpoint
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))
```

### Error Rate

```promql
# 5xx error rate (percentage)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count[5m]))

# 4xx error rate
sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m]))
  / sum(rate(http_server_requests_seconds_count[5m]))
```

### JVM

```promql
# Heap memory used (all pools)
sum(jvm_memory_used_bytes{area="heap"})

# Non-heap memory used
sum(jvm_memory_used_bytes{area="nonheap"})

# Live thread count
jvm_threads_live_threads

# GC pause rate
rate(jvm_gc_pause_seconds_sum[5m])
```

### Database

```promql
# Active connections
hikaricp_connections_active

# Pending connection requests (should be 0 normally)
hikaricp_connections_pending

# Average connection acquire time
rate(hikaricp_connections_acquire_seconds_sum[5m])
  / rate(hikaricp_connections_acquire_seconds_count[5m])
```

### Cache

```promql
# Redis manual cache hit ratio
redis_cache_hits_total / (redis_cache_hits_total + redis_cache_misses_total)

# Spring @Cacheable hit ratio (by cache name)
cache_gets_total{result="hit"} / (cache_gets_total{result="hit"} + cache_gets_total{result="miss"})

# Cache operations per second
rate(redis_cache_puts_total[5m])
```

---

## Docker Compose Services

### Prometheus

```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: spring-prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    - prometheus_data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.retention.time=15d'   # 15-day retention
    - '--web.enable-lifecycle'               # Enable hot-reload
```

### Grafana

```yaml
grafana:
  image: grafana/grafana:latest
  container_name: spring-grafana
  ports:
    - "3000:3000"
  environment:
    GF_SECURITY_ADMIN_USER: admin
    GF_SECURITY_ADMIN_PASSWORD: admin
  volumes:
    - grafana_data:/var/lib/grafana
    - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
  depends_on:
    prometheus:
      condition: service_healthy
```

---

## Common Tasks

### Reload Prometheus config without restart

```bash
curl -X POST http://localhost:9090/-/reload
```

### Check if Prometheus is scraping the app

1. Open http://localhost:9090/targets
2. The `spring-boot-app` target should show **State: UP**
3. If it shows **DOWN**, check:
   - Is the app running? (`docker ps`)
   - Can you access http://localhost:8082/actuator/prometheus directly?

### Add a custom dashboard to Grafana

1. Create a JSON dashboard file
2. Save it to `monitoring/grafana/provisioning/dashboards/json/`
3. Restart Grafana: `docker-compose restart grafana`

Or import directly in the Grafana UI:
1. Go to **Dashboards** → **New** → **Import**
2. Paste a dashboard ID from [Grafana Dashboards](https://grafana.com/grafana/dashboards/)
3. Popular Spring Boot dashboards: `4701`, `12900`, `11378`

### View raw metrics from the app

```bash
# All metrics (text format)
curl http://localhost:8082/actuator/prometheus

# Filter for HTTP metrics
curl -s http://localhost:8082/actuator/prometheus | grep http_server

# Filter for HikariCP metrics
curl -s http://localhost:8082/actuator/prometheus | grep hikaricp

# Filter for Redis cache metrics
curl -s http://localhost:8082/actuator/prometheus | grep redis_cache
```

### Reset Grafana data

```bash
docker-compose down
docker volume rm spring-monolith-template_grafana_data
docker-compose up -d grafana
```

### Reset Prometheus data

```bash
docker-compose down
docker volume rm spring-monolith-template_prometheus_data
docker-compose up -d prometheus
```

---

## Troubleshooting

### Grafana shows "No Data"

1. **Check Prometheus target**: http://localhost:9090/targets → is `spring-boot-app` UP?
2. **Check raw metrics**: `curl http://localhost:8082/actuator/prometheus` → do you see metrics?
3. **Check time range**: In Grafana, set time range to "Last 15 minutes"
4. **Check datasource**: Grafana → Settings → Data Sources → Prometheus → "Test" button

### Cache panels show "No Data"

Cache metrics only appear after cache operations occur:

- **`@Cacheable` metrics** (`cache_gets_total`): Only appear when you use `@Cacheable` annotations in your service methods
- **Redis manual metrics** (`redis_cache_hits_total`): Only appear after `RedisCacheService.get()` / `set()` are called

Make some API requests first, then check again.

### Prometheus can't reach the app

If running **Java locally** but **Prometheus in Docker**:

1. Edit `monitoring/prometheus/prometheus.yml`
2. Uncomment the `host.docker.internal` target:

```yaml
static_configs:
  - targets: ['host.docker.internal:8082']
    labels:
      application: 'spring-monolith-template'
      environment: 'local'
```

3. Reload: `curl -X POST http://localhost:9090/-/reload`

### Metrics are stale / not updating

- Prometheus scrapes every **15 seconds** — wait at least 30s for new data
- Grafana auto-refreshes every **15 seconds** (configurable in dashboard settings)
- Check Prometheus scrape status: http://localhost:9090/targets

---

## Production Considerations

### Security

- **Never expose `/actuator/prometheus` publicly** — restrict via firewall/network policy
- In production, Prometheus should scrape via internal network only
- Grafana should be behind a reverse proxy with proper authentication
- Change default Grafana credentials (`admin`/`admin`)

### Retention & Storage

| Setting | Default | Config Location |
|---------|---------|-----------------|
| Prometheus retention | 15 days | `docker-compose.yml` → `--storage.tsdb.retention.time` |
| Prometheus storage | Docker volume | `prometheus_data` volume |
| Grafana storage | Docker volume | `grafana_data` volume |

### Alerting (optional)

Grafana supports alerting out of the box:

1. Open any dashboard panel → **Edit** → **Alert** tab
2. Set conditions (e.g., "Error rate > 5% for 5 minutes")
3. Configure notification channels (email, Slack, PagerDuty)

Example alert conditions:
- Error rate > 5% for 5 minutes
- P95 latency > 2 seconds for 5 minutes
- HikariCP pending connections > 0 for 1 minute
- JVM heap usage > 85% for 10 minutes
- Application DOWN for 1 minute

---

## File Structure

```
spring-monolith-template/
├── monitoring/
│   ├── prometheus/
│   │   └── prometheus.yml                  # Scrape config (targets, intervals)
│   └── grafana/
│       └── provisioning/
│           ├── datasources/
│           │   └── prometheus.yml          # Auto-connect to Prometheus
│           └── dashboards/
│               ├── dashboards.yml          # Dashboard loader config
│               └── json/
│                   └── spring-boot-overview.json  # Pre-built dashboard
├── src/main/resources/
│   ├── application.yaml                    # Base monitoring config
│   ├── application-dev.yaml                # Dev: extra endpoints exposed
│   └── application-prod.yml               # Prod: locked-down endpoints
├── docker-compose.yml                      # Prometheus + Grafana services
└── pom.xml                                 # micrometer-registry-prometheus dependency
```
