# Redis Cache

## Overview

Redis caching implementation with simple set, get, delete operations and annotation-based caching.

## Folder Structure

```
common/
└── redis/
    ├── RedisKey.java           # Key prefixes enum
    ├── RedisCacheService.java  # Main service (set, get, delete)
    ├── RedisCached.java        # @RedisCached annotation
    ├── RedisCacheEvict.java    # @RedisCacheEvict annotation
    └── RedisCacheAspect.java   # Aspect handler

config/
└── redis/
    └── RedisConfig.java        # Redis connection configuration
```

---

## Usage

### 1. Inject RedisCacheService

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final RedisCacheService cacheService;
    
    public User getUser(String userId) {
        // Try cache first
        return cacheService.getObject(CacheKey.USER.key(userId), User.class)
            .orElseGet(() -> {
                User user = userRepository.findById(userId).orElseThrow();
                cacheService.setObject(CacheKey.USER.key(userId), user, Duration.ofHours(1));
                return user;
            });
    }
}
```

### 2. Simple String Operations

```java
// Set value
cacheService.set("key", "value");
cacheService.set("key", "value", Duration.ofMinutes(30));
cacheService.set("key", "value", 1800);  // 1800 seconds

// Get value
Optional<String> value = cacheService.get("key");
String value = cacheService.getOrDefault("key", "default");

// Delete
cacheService.delete("key");
cacheService.deleteByPattern("user:*");  // Delete all user keys
```

### 3. Object Operations (JSON)

```java
// Set object (auto JSON serialization)
UserDto user = new UserDto("John", "john@example.com");
cacheService.setObject("user:123", user, Duration.ofHours(1));

// Get object (auto JSON deserialization)
Optional<UserDto> user = cacheService.getObject("user:123", UserDto.class);
```

### 4. Using RedisKey Enum

```java
// Instead of hardcoding strings
cacheService.set("user:123:profile", data);

// Use RedisKey for consistency
cacheService.set(RedisKey.USER_PROFILE.key("123"), data);
cacheService.set(RedisKey.USER.key("123", "profile"), data);  // user:123:profile

// Available keys
RedisKey.USER          // "user"
RedisKey.USER_SESSION  // "user:session"
RedisKey.USER_PROFILE  // "user:profile"
RedisKey.AUTH_TOKEN    // "auth:token"
RedisKey.REFRESH_TOKEN // "refresh:token"
RedisKey.OTP           // "otp"
RedisKey.TEMP          // "temp"
```

---

## Annotation-Based Caching

### @RedisCached - Cache Method Results

```java
@Service
public class ProductService {

    @RedisCached(key = "product", ttlSeconds = 3600)  // Cache for 1 hour
    public Product getProduct(String productId) {
        // Result cached as "product:{productId}"
        return productRepository.findById(productId).orElseThrow();
    }
    
    @RedisCached(key = "products:category", ttlSeconds = 1800)
    public List<Product> getByCategory(String category) {
        // Result cached as "products:category:{category}"
        return productRepository.findByCategory(category);
    }
}
```

### @RedisCacheEvict - Invalidate Cache

```java
@Service
public class ProductService {

    @RedisCacheEvict(key = "product")
    public void updateProduct(String productId, UpdateRequest request) {
        // Evicts "product:{productId}" after update
        productRepository.update(productId, request);
    }
    
    @RedisCacheEvict(key = "product", allEntries = true)
    public void clearProductCache() {
        // Evicts all "product:*" keys
    }
    
    @RedisCacheEvict(key = "product", beforeInvocation = true)
    public void deleteProduct(String productId) {
        // Evicts cache BEFORE deletion
        productRepository.delete(productId);
    }
}
```

---

## Hash Operations

```java
// Set hash fields
cacheService.hashSet("user:123", "name", "John");
cacheService.hashSet("user:123", "email", "john@example.com");

// Or set multiple at once
cacheService.hashSetAll("user:123", Map.of(
    "name", "John",
    "email", "john@example.com",
    "status", "active"
));

// Get hash field
String name = cacheService.hashGet("user:123", "name").orElse("");

// Get all fields
Map<String, String> userData = cacheService.hashGetAll("user:123");

// Delete hash field
cacheService.hashDelete("user:123", "email");
```

---

## List Operations

```java
// Add to list
cacheService.listPush("notifications:user123", "New message");
cacheService.listPush("notifications:user123", "Order shipped");

// Get list items
List<String> notifications = cacheService.listRange("notifications:user123", 0, 10);

// Get list size
long count = cacheService.listSize("notifications:user123");
```

---

## Set Operations

```java
// Add to set
cacheService.setAdd("online:users", "user1", "user2", "user3");

// Check membership
boolean isOnline = cacheService.setIsMember("online:users", "user1");

// Get all members
Set<String> onlineUsers = cacheService.setMembers("online:users");

// Remove from set
cacheService.setRemove("online:users", "user1");
```

---

## Counter Operations

```java
// Increment
long views = cacheService.increment("page:views:home");
long views = cacheService.incrementBy("page:views:home", 5);

// Decrement
long stock = cacheService.decrement("product:stock:123");
```

---

## Utility Methods

```java
// Check if key exists
boolean exists = cacheService.exists("user:123");

// Set expiration on existing key
cacheService.expire("user:123", Duration.ofHours(2));

// Get remaining TTL
Optional<Long> ttl = cacheService.getTtl("user:123");  // seconds

// Set only if not exists (useful for locks)
boolean acquired = cacheService.setIfAbsent("lock:process1", "locked", Duration.ofMinutes(5));

// Get all keys matching pattern
Set<String> userKeys = cacheService.keys("user:*");
```

---

## Complete Example

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final RedisCacheService cacheService;
    
    // Method 1: Manual caching
    public User getUser(UUID userId) {
        String cacheKey = RedisKey.USER.key(userId.toString());
        
        // Try cache
        return cacheService.getObject(cacheKey, User.class)
            .orElseGet(() -> {
                log.info("Cache miss for user: {}", userId);
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
                cacheService.setObject(cacheKey, user, Duration.ofHours(1));
                return user;
            });
    }
    
    // Method 2: Annotation-based caching
    @RedisCached(key = "user:profile", ttlSeconds = 1800)
    public UserProfile getProfile(UUID userId) {
        return userRepository.findProfileById(userId);
    }
    
    // Invalidate cache on update
    @RedisCacheEvict(key = "user")
    public void updateUser(UUID userId, UpdateRequest request) {
        userRepository.update(userId, request);
    }
    
    // Clear all user caches
    @RedisCacheEvict(key = "user", allEntries = true)
    public void refreshAllUsers() {
        log.info("Cleared all user caches");
    }
}
```

---

## Configuration

### application.yaml

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: yourStrongPassword123
      timeout: 60000
```

### Cache TTL Guidelines

| Data Type | Recommended TTL |
|-----------|-----------------|
| User profile | 30 min - 1 hour |
| Static config | 1 hour - 24 hours |
| Session data | 30 min |
| OTP codes | 5 - 10 min |
| Rate limit | 1 min - 1 hour |
| Temporary data | As needed |

---

## Best Practices

1. **Use RedisKey enum** - Avoid hardcoded strings
2. **Set appropriate TTL** - Don't cache forever
3. **Invalidate on update** - Use @RedisCacheEvict
4. **Handle cache miss gracefully** - Always have fallback
5. **Log cache operations** - Debug enabled for troubleshooting
6. **Use objects for complex data** - JSON serialization handles it
