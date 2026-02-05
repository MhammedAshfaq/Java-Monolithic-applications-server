# Rate Limiting

IP-based rate limiting using Redis to protect your API from abuse and brute-force attacks.

## Overview

The rate limiter provides three tiers of protection:

| Type | Default Limit | Use Case |
|------|---------------|----------|
| `SHORT_TERM` | 100 req/min | Normal API endpoints, burst protection |
| `LONG_TERM` | 1000 req/hour | Overall usage cap, abuse prevention |
| `STRICT` | 5 req/min | Login, password reset, sensitive operations |

## Configuration

### application.yaml

```yaml
app:
  rate-limit:
    enabled: true
    short-term:
      max-requests: 100
      window-seconds: 60
    long-term:
      max-requests: 1000
      window-seconds: 3600
    strict:
      max-requests: 5
      window-seconds: 60
```

### Disable Rate Limiting (Development)

```yaml
app:
  rate-limit:
    enabled: false
```

## How It Works

### Automatic Protection

All `/api/**` endpoints are automatically protected with both `SHORT_TERM` and `LONG_TERM` limits via `RateLimitFilter`.

**Excluded paths:**
- `/actuator/**` - Health checks
- `/health/**` - Health endpoints  
- `/dev/**` - Developer dashboard
- `/apidocs/**` - Swagger UI
- `/v1/api-docs/**` - OpenAPI spec

### Response Headers

Every API response includes rate limit headers:

```
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 45
```

### Rate Limit Exceeded (HTTP 429)

When limit is exceeded, the API returns:

```json
{
    "timestamp": "2026-02-05T12:00:00",
    "status": 429,
    "error": "Too Many Requests",
    "message": "Rate limit exceeded. Please try again in 30 seconds.",
    "retryAfter": 30
}
```

With headers:
```
Retry-After: 30
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 30
```

## Usage

### 1. Annotation-Based (Recommended)

Apply stricter limits to specific endpoints using `@RateLimit`:

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Strict rate limit: 5 requests per minute
    @RateLimit(type = RateLimitType.STRICT)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // ...
    }

    // Strict limit with custom key (separate counter)
    @RateLimit(type = RateLimitType.STRICT, key = "password-reset")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetRequest request) {
        // ...
    }

    // Short-term limit: 100 requests per minute
    @RateLimit(type = RateLimitType.SHORT_TERM)
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        // ...
    }
}
```

### 2. Programmatic Usage

Inject `RateLimiterService` for manual rate limit checks:

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final RateLimiterService rateLimiterService;

    public void processRequest(String clientIp) {
        // Check rate limit
        var result = rateLimiterService.checkLimit(clientIp, RateLimitType.SHORT_TERM);
        
        if (!result.allowed()) {
            throw new TooManyRequestsException(
                "Rate limit exceeded. Try again in " + result.resetSeconds() + " seconds"
            );
        }
        
        // Process request...
    }
    
    // Check multiple limits at once
    public void sensitiveOperation(String clientIp) {
        var result = rateLimiterService.checkLimits(
            clientIp,
            RateLimitType.STRICT,
            RateLimitType.LONG_TERM
        );
        
        if (!result.allowed()) {
            // Handle rate limit exceeded
        }
    }
}
```

### 3. Admin Operations

```java
@Service
@RequiredArgsConstructor  
public class AdminService {
    
    private final RateLimiterService rateLimiterService;

    // Get current request count for IP
    public void checkUserStatus(String ip) {
        Optional<Integer> count = rateLimiterService.getCurrentCount(ip, RateLimitType.SHORT_TERM);
        // ...
    }

    // Reset rate limit for specific IP (unblock user)
    public void unblockUser(String ip) {
        rateLimiterService.resetLimit(ip, RateLimitType.STRICT);
    }

    // Reset all rate limits for IP
    public void fullyUnblockUser(String ip) {
        rateLimiterService.resetAllLimits(ip);
    }
}
```

## Rate Limit Types

### SHORT_TERM (Burst Protection)

- **Default:** 100 requests per 60 seconds
- **Use for:** Normal API endpoints
- **Purpose:** Prevents sudden bursts of requests

### LONG_TERM (Usage Cap)

- **Default:** 1000 requests per 3600 seconds (1 hour)
- **Use for:** Overall API usage
- **Purpose:** Prevents sustained abuse over time

### STRICT (Sensitive Operations)

- **Default:** 5 requests per 60 seconds
- **Use for:** Login, registration, password reset, OTP verification
- **Purpose:** Prevents brute-force attacks

## Redis Keys

Rate limit data is stored in Redis with the following key pattern:

```
rate_limit:{type}:{ip}

Examples:
rate_limit:short_term:192.168.1.100
rate_limit:long_term:192.168.1.100
rate_limit:strict:192.168.1.100
```

With custom keys:
```
rate_limit:strict:192.168.1.100:login
rate_limit:strict:192.168.1.100:password-reset
```

## IP Detection

The rate limiter extracts client IP from (in order):

1. `X-Forwarded-For` header (first IP)
2. `X-Real-IP` header
3. `Proxy-Client-IP` header
4. `WL-Proxy-Client-IP` header
5. `request.getRemoteAddr()` (fallback)

**Note:** If behind a reverse proxy (nginx, load balancer), ensure proxy headers are configured correctly.

## Architecture

```
┌─────────────────┐
│  HTTP Request   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ RateLimitFilter │ ← Applies SHORT_TERM + LONG_TERM to all /api/**
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ RateLimitAspect │ ← Handles @RateLimit annotation on methods
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│RateLimiterService│ ← Core logic, Redis operations
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│     Redis       │ ← Stores counters with TTL
└─────────────────┘
```

## Files

| File | Description |
|------|-------------|
| `common/ratelimit/RateLimitType.java` | Enum defining rate limit types |
| `common/ratelimit/RateLimitConfig.java` | Configuration from YAML |
| `common/ratelimit/RateLimiterService.java` | Core rate limiting logic |
| `common/ratelimit/RateLimitFilter.java` | HTTP filter for automatic protection |
| `common/ratelimit/RateLimit.java` | Annotation for method-level limits |
| `common/ratelimit/RateLimitAspect.java` | Aspect handling @RateLimit |

## Best Practices

1. **Always use STRICT for auth endpoints** - Login, register, password reset, OTP
2. **Use custom keys for separate counters** - `@RateLimit(key = "login")` vs `@RateLimit(key = "register")`
3. **Fail open on Redis errors** - The service allows requests if Redis is unavailable
4. **Monitor rate limit hits** - Log warnings when limits are exceeded
5. **Adjust limits per environment** - Lower limits in production, higher in development

## Troubleshooting

### Rate limits not working

1. Check Redis connection: `redis-cli PING`
2. Verify `app.rate-limit.enabled: true` in config
3. Check if endpoint is in excluded paths

### Users getting blocked incorrectly

1. Check if behind proxy - ensure `X-Forwarded-For` is set
2. Verify Redis TTL is working: `redis-cli TTL rate_limit:short_term:IP`
3. Reset user limit: `rateLimiterService.resetAllLimits(ip)`

### View current rate limit status

```bash
# Check all rate limit keys
redis-cli KEYS "rate_limit:*"

# Check specific IP count
redis-cli GET "rate_limit:short_term:192.168.1.100"

# Check TTL
redis-cli TTL "rate_limit:short_term:192.168.1.100"
```
