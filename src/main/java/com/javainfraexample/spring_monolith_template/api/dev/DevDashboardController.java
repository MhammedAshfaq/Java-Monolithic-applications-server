package com.javainfraexample.spring_monolith_template.api.dev;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Optional;

/**
 * Developer Dashboard — only active when {@code dev} profile is enabled.
 * Not loaded in production. Serves / and /dev with links to Swagger, RabbitMQ, Actuator, Health, DB/Redis health.
 */
@RestController
@RequestMapping
@Profile("dev")
public class DevDashboardController {

    private static final CacheControl NO_CACHE = CacheControl.noStore().mustRevalidate();

    private final DataSource dataSource;
    private final Optional<RedisConnectionFactory> redisConnectionFactory;

    public DevDashboardController(DataSource dataSource,
                                  Optional<RedisConnectionFactory> redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Value("${server.port:8082}")
    private String serverPort;

    @Value("${app.dev-dashboard.rabbitmq-management-url:http://localhost:15672}")
    private String rabbitmqManagementUrl;

    @Value("${springdoc.swagger-ui.path:/apidocs}")
    private String swaggerUiPath;

    private static final String TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Dev Tools — Development only</title>
          <link rel="preconnect" href="https://fonts.googleapis.com">
          <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
          <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&display=swap" rel="stylesheet">
          <style>
            :root {
              --bg: #0c0f17;
              --surface: #151922;
              --surface-hover: #1c212c;
              --border: #2a3142;
              --text: #e6e9f0;
              --text-muted: #8b92a5;
              --accent: #5b8def;
              --accent-hover: #7ba3f4;
              --success: #34c759;
              --radius: 12px;
              --radius-sm: 8px;
              --shadow: 0 4px 24px rgba(0,0,0,0.35);
              --transition: 0.2s ease;
            }
            *, *::before, *::after { box-sizing: border-box; }
            body {
              font-family: 'DM Sans', system-ui, -apple-system, sans-serif;
              margin: 0;
              padding: 0;
              background: var(--bg);
              color: var(--text);
              min-height: 100vh;
              line-height: 1.5;
              -webkit-font-smoothing: antialiased;
            }
            .wrap {
              max-width: 720px;
              margin: 0 auto;
              padding: clamp(1.5rem, 5vw, 3rem);
            }
            .header {
              margin-bottom: 2rem;
            }
            .badge {
              display: inline-flex;
              align-items: center;
              gap: 0.35rem;
              font-size: 0.7rem;
              font-weight: 600;
              text-transform: uppercase;
              letter-spacing: 0.06em;
              color: var(--accent);
              background: rgba(91, 141, 239, 0.12);
              border: 1px solid rgba(91, 141, 239, 0.25);
              padding: 0.35rem 0.65rem;
              border-radius: 6px;
              margin-bottom: 0.75rem;
            }
            .badge::before { content: ""; width: 6px; height: 6px; background: var(--success); border-radius: 50%%; animation: pulse 2s ease-in-out infinite; }
            @keyframes pulse { 0%%, 100%% { opacity: 1; } 50%% { opacity: 0.5; } }
            h1 {
              font-size: clamp(1.6rem, 4vw, 2rem);
              font-weight: 700;
              margin: 0 0 0.4rem 0;
              color: #fff;
              letter-spacing: -0.02em;
            }
            .sub {
              color: var(--text-muted);
              font-size: 0.95rem;
              margin: 0;
              max-width: 36em;
            }
            .grid {
              display: grid;
              grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
              gap: 0.875rem;
              list-style: none;
              padding: 0;
              margin: 0 0 2rem 0;
            }
            .card {
              display: block;
              padding: 1.1rem 1.25rem;
              background: var(--surface);
              border: 1px solid var(--border);
              border-radius: var(--radius);
              color: var(--accent);
              text-decoration: none;
              font-weight: 500;
              font-size: 0.95rem;
              transition: background var(--transition), border-color var(--transition), transform var(--transition);
              box-shadow: var(--shadow);
            }
            .card:hover {
              background: var(--surface-hover);
              border-color: var(--accent);
              color: var(--accent-hover);
              transform: translateY(-2px);
            }
            .card:focus-visible {
              outline: 2px solid var(--accent);
              outline-offset: 2px;
            }
            .card.external::after {
              content: " ↗";
              font-size: 0.8em;
              opacity: 0.8;
            }
            .card-label {
              display: block;
              font-size: 0.75rem;
              color: var(--text-muted);
              font-weight: 400;
              margin-top: 0.25rem;
            }
            .section {
              margin-top: 2rem;
              padding-top: 1.5rem;
              border-top: 1px solid var(--border);
            }
            .section-title {
              font-size: 0.75rem;
              font-weight: 600;
              text-transform: uppercase;
              letter-spacing: 0.06em;
              color: var(--text-muted);
              margin: 0 0 0.75rem 0;
            }
            .pills {
              display: flex;
              flex-wrap: wrap;
              gap: 0.5rem;
            }
            .pills a {
              display: inline-block;
              padding: 0.4rem 0.85rem;
              font-size: 0.8rem;
              font-family: inherit;
              color: var(--text-muted);
              background: var(--surface);
              border: 1px solid var(--border);
              border-radius: 999px;
              text-decoration: none;
              transition: background var(--transition), color var(--transition), border-color var(--transition);
            }
            .pills a:hover {
              color: var(--accent);
              background: var(--surface-hover);
              border-color: var(--accent);
            }
            .pills a:focus-visible {
              outline: 2px solid var(--accent);
              outline-offset: 2px;
            }
          </style>
        </head>
        <body>
          <div class="wrap">
            <header class="header">
              <span class="badge" aria-hidden="true">Development only</span>
              <h1>Dev Tools</h1>
              <p class="sub">Quick access to Swagger, RabbitMQ, Redis, database health, and Actuator. Not available in production.</p>
            </header>
            <ul class="grid" role="list">
              <li><a class="card" href="%s">Swagger Dashboard<span class="card-label">API docs · Try it out</span></a></li>
              <li><a class="card external" href="%s" target="_blank" rel="noopener noreferrer">RabbitMQ Dashboard<span class="card-label">Queues · like BullMQ</span></a></li>
              <li><a class="card" href="%s">Health<span class="card-label">App health</span></a></li>
              <li><a class="card" href="%s">Database health<span class="card-label">Test DB connection</span></a></li>
              <li><a class="card" href="%s">Redis health<span class="card-label">Test Redis connection</span></a></li>
              <li><a class="card" href="%s">Actuator<span class="card-label">JSON index · _links</span></a></li>
            </ul>
            <section class="section" aria-label="Actuator endpoints">
              <h2 class="section-title">Actuator (JSON)</h2>
              <div class="pills">
                <a href="%s">health</a>
                <a href="%s">info</a>
                <a href="%s">metrics</a>
                <a href="%s">env</a>
              </div>
            </section>
          </div>
        </body>
        </html>
        """;

    /**
     * Dev dashboard: same page at / and /dev so localhost:8082 shows it directly (like NestJS).
     */
    @GetMapping(value = { "/", "/dev" }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> devDashboard() {
        String base = "http://localhost:" + serverPort;
        String actuatorBase = base + "/actuator";
        String dbHealthUrl = base + "/dev/db-health";
        String redisHealthUrl = base + "/dev/redis-health";
        String html = TEMPLATE.formatted(
            base + swaggerUiPath,
            rabbitmqManagementUrl,
            base + "/health",
            dbHealthUrl,
            redisHealthUrl,
            actuatorBase,
            actuatorBase + "/health",
            actuatorBase + "/info",
            actuatorBase + "/metrics",
            actuatorBase + "/env"
        );
        return ResponseEntity.ok()
            .cacheControl(NO_CACHE)
            .body(html);
    }

    /**
     * Dev-only: test database connectivity (e.g. run SELECT 1). Use from dev tools to verify DB is up.
     */
    @GetMapping(value = "/dev/db-health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            if (valid) {
                return ResponseEntity.ok(Map.of(
                    "status", "up",
                    "database", "connected",
                    "message", "Database connection is healthy."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "down",
                "database", "disconnected",
                "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            ));
        }
        return ResponseEntity.status(503).body(Map.of(
            "status", "down",
            "database", "invalid",
            "message", "Connection validation failed."
        ));
    }

    /**
     * Dev-only: test Redis connectivity (PING). Returns "not configured" when Redis is not available.
     */
    @GetMapping(value = "/dev/redis-health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> redisHealth() {
        if (redisConnectionFactory.isEmpty()) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "down",
                "redis", "not configured",
                "message", "Redis is not configured or dependency not present."
            ));
        }
        try (RedisConnection conn = redisConnectionFactory.get().getConnection()) {
            String pong = conn.ping();
            return ResponseEntity.ok(Map.of(
                "status", "up",
                "redis", "connected",
                "message", "Redis connection is healthy.",
                "ping", pong != null ? pong : "PONG"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "down",
                "redis", "disconnected",
                "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            ));
        }
    }
}
