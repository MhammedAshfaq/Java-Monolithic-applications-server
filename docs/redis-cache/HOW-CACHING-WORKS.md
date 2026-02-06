# How Redis Caching Works

This document explains the Redis caching implementation in the User Service using a step-by-step breakdown.

## Overview

Redis caching follows a **Cache-Aside (Lazy Loading)** pattern:
1. Check cache first
2. If found (HIT) → return cached data
3. If not found (MISS) → fetch from database, store in cache, return data

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Client    │─────▶│   Service   │─────▶│    Redis    │
│  (Request)  │      │  (findById) │      │   (Cache)   │
└─────────────┘      └──────┬──────┘      └─────────────┘
                           │
                           │ Cache MISS
                           ▼
                    ┌─────────────┐
                    │  PostgreSQL │
                    │  (Database) │
                    └─────────────┘
```

## Code Breakdown: `UserService.findById()`

```java
public ApiResponseDto<UserResponse> findById(UUID id) {
    // Step 1: Build cache key
    String cacheKey = RedisKey.USER.key(id.toString());

    // Step 2: Try to get from cache
    Optional<UserResponse> cachedUser = cacheService.getObject(cacheKey, UserResponse.class);
    
    // Step 3: Check if cache HIT
    if (cachedUser.isPresent()) {
        log.debug("Cache HIT for user: {}", id);
        return ApiResponseDto.success("User retrieved successfully", cachedUser.get());
    }
    
    // Step 4: Cache MISS - fetch from database
    log.debug("Cache MISS for user: {}", id);
    User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    // Step 5: Convert entity to DTO
    UserResponse response = UserResponse.from(user);
    
    // Step 6: Store in cache for future requests
    cacheService.setObject(cacheKey, response, USER_CACHE_TTL);
    
    return ApiResponseDto.success("User retrieved successfully", response);
}
```

## Step-by-Step Explanation

### Step 1: Build Cache Key

```java
String cacheKey = RedisKey.USER.key(id.toString());
// Result: "user:550e8400-e29b-41d4-a716-446655440002"
```

The `RedisKey` enum provides consistent key prefixes:
- `USER` prefix → `"user:"`
- Combined with UUID → `"user:{uuid}"`

This ensures:
- Organized keys in Redis
- No key collisions between different data types
- Easy pattern matching for bulk operations (e.g., `user:*`)

### Step 2: Try to Get from Cache

```java
Optional<UserResponse> cachedUser = cacheService.getObject(cacheKey, UserResponse.class);
```

**What happens internally in `RedisCacheService.getObject()`:**

```java
public <T> Optional<T> getObject(String key, Class<T> clazz) {
    // 2a. Call Redis GET command
    Optional<String> cached = get(key);  // Returns JSON string or empty
    
    // 2b. If empty, return empty Optional
    if (cached.isEmpty()) {
        log.debug("Redis GET object: {} -> MISS", key);
        return Optional.empty();
    }
    
    // 2c. Deserialize JSON to UserResponse object
    try {
        log.debug("Redis GET object: {} -> HIT", key);
        return Optional.of(objectMapper.readValue(cached.get(), clazz));
    } catch (JsonProcessingException e) {
        log.error("Redis GET object deserialization failed for key: {}", key, e);
        return Optional.empty();
    }
}
```

**Redis Command:** `GET user:550e8400-e29b-41d4-a716-446655440002`

**Stored JSON:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-02-05T18:25:59.22675",
  "updatedAt": "2026-02-06T11:36:43.169783",
  "lastLoginAt": null
}
```

### Step 3: Check Cache HIT

```java
if (cachedUser.isPresent()) {
    log.debug("Cache HIT for user: {}", id);
    return ApiResponseDto.success("User retrieved successfully", cachedUser.get());
}
```

- **Cache HIT**: Data found in Redis → Return immediately (fast!)
- **No database query needed** → Reduced latency & database load

### Step 4: Cache MISS - Fetch from Database

```java
log.debug("Cache MISS for user: {}", id);
User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
```

- Data not in Redis → Query PostgreSQL
- If not found in DB → Throw 404 exception
- **SQL Query:** `SELECT * FROM users WHERE id = ?`

### Step 5: Convert Entity to DTO

```java
UserResponse response = UserResponse.from(user);
```

- Convert JPA `User` entity to `UserResponse` DTO
- DTOs are safe for caching (no lazy-loading issues)
- Excludes sensitive fields like `password`

### Step 6: Store in Cache

```java
cacheService.setObject(cacheKey, response, USER_CACHE_TTL);
// USER_CACHE_TTL = Duration.ofMinutes(30)
```

**What happens internally in `RedisCacheService.setObject()`:**

```java
public <T> void setObject(String key, T value, Duration ttl) {
    // 6a. Serialize object to JSON
    String json = objectMapper.writeValueAsString(value);
    
    // 6b. Store in Redis with TTL
    redisTemplate.opsForValue().set(key, json, ttl);
    log.debug("Redis SET: {} (TTL: {})", key, ttl);
}
```

**Redis Command:** `SETEX user:550e8400-... 1800 "{json}"`

- Key: `user:550e8400-e29b-41d4-a716-446655440002`
- Value: JSON string
- TTL: 1800 seconds (30 minutes)

## Flow Diagrams

### First Request (Cache MISS)

```
Request: GET /api/users/550e8400-...
           │
           ▼
    ┌──────────────┐
    │ UserService  │
    │  findById()  │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │ Redis Cache  │──── GET user:550e8400-...
    │              │──── Returns: (nil) - MISS
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │  PostgreSQL  │──── SELECT * FROM users WHERE id = ?
    │              │──── Returns: User entity
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │ Redis Cache  │──── SETEX user:550e8400-... 1800 "{json}"
    │              │──── Stores for 30 minutes
    └──────┬───────┘
           │
           ▼
    Response: UserResponse JSON
```

### Subsequent Requests (Cache HIT)

```
Request: GET /api/users/550e8400-...
           │
           ▼
    ┌──────────────┐
    │ UserService  │
    │  findById()  │
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │ Redis Cache  │──── GET user:550e8400-...
    │              │──── Returns: "{json}" - HIT
    └──────┬───────┘
           │
           │ (No database query!)
           │
           ▼
    Response: UserResponse JSON
```

## Cache Invalidation

When user data is updated or deleted, the cache must be invalidated:

### On Update

```java
@Transactional
public ApiResponseDto<UserResponse> update(UUID id, UpdateUserRequest request) {
    // ... update logic ...
    
    // Invalidate cache after update
    cacheService.delete(RedisKey.USER.key(id.toString()));
    
    return ApiResponseDto.success("User updated successfully", UserResponse.from(savedUser));
}
```

### On Delete

```java
@Transactional
public ApiResponseDto<Void> delete(UUID id) {
    // ... delete logic ...
    
    // Invalidate cache after delete
    cacheService.delete(RedisKey.USER.key(id.toString()));
    
    return ApiResponseDto.<Void>success("User deleted successfully", null);
}
```

**Why invalidate instead of update?**
- Simpler logic
- Avoids race conditions
- Next read will fetch fresh data and re-cache

## Performance Comparison

| Scenario | Latency | Database Load |
|----------|---------|---------------|
| Without Cache | ~50-100ms | High (every request) |
| Cache HIT | ~1-5ms | None |
| Cache MISS | ~50-100ms | Once per TTL period |

## TTL (Time-To-Live) Strategy

```java
private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
```

| Data Type | Recommended TTL | Reason |
|-----------|-----------------|--------|
| User Profile | 30 minutes | Changes infrequently |
| Session Data | 15-60 minutes | Security consideration |
| Configuration | 1-24 hours | Rarely changes |
| Frequently Updated | 1-5 minutes | Balance freshness vs performance |

## Debug Logs

With `DEBUG` logging enabled, you'll see:

```
# Cache MISS (first request)
DEBUG UserService     : Cache MISS for user: 550e8400-...
DEBUG RedisCacheService: Redis GET: user:550e8400-... -> MISS
DEBUG RedisCacheService: Redis SET: user:550e8400-... (TTL: PT30M)

# Cache HIT (subsequent requests)
DEBUG RedisCacheService: Redis GET: user:550e8400-... -> HIT
DEBUG RedisCacheService: Redis GET object: user:550e8400-... -> HIT
DEBUG UserService     : Cache HIT for user: 550e8400-...
```

## Key Concepts Summary

| Concept | Description |
|---------|-------------|
| **Cache-Aside Pattern** | Application manages cache reads/writes |
| **Cache Key** | Unique identifier (e.g., `user:{uuid}`) |
| **TTL** | Time-To-Live - auto-expiration of cached data |
| **Cache HIT** | Data found in cache |
| **Cache MISS** | Data not in cache, fetch from DB |
| **Cache Invalidation** | Removing stale data on update/delete |
| **JSON Serialization** | Objects stored as JSON strings in Redis |

## Best Practices

1. **Cache DTOs, not Entities** - Avoid lazy-loading issues
2. **Set appropriate TTL** - Balance freshness vs performance
3. **Invalidate on write** - Keep cache consistent
4. **Use meaningful key prefixes** - Easy debugging and management
5. **Handle cache failures gracefully** - Fall back to database
6. **Don't cache sensitive data** - Or encrypt if necessary
