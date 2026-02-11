package com.javainfraexample.spring_monolith_template.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis Cache Service - Provides simple set, get, delete operations.
 * 
 * Usage Examples:
 * <pre>
 * // Simple string operations
 * cacheService.set("key", "value");
 * cacheService.set("key", "value", Duration.ofMinutes(30));
 * String value = cacheService.get("key").orElse("default");
 * cacheService.delete("key");
 * 
 * // With RedisKey enum
 * cacheService.set(RedisKey.USER.key(userId), userData, Duration.ofHours(1));
 * 
 * // Object operations (JSON serialization)
 * cacheService.setObject("user:123", userDto, Duration.ofHours(1));
 * UserDto user = cacheService.getObject("user:123", UserDto.class).orElse(null);
 * 
 * // Hash operations
 * cacheService.hashSet("user:123", "name", "John");
 * String name = cacheService.hashGet("user:123", "name").orElse("");
 * </pre>
 */
@Slf4j
@Service
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // Micrometer counters — visible in Grafana as redis_cache_hits_total, etc.
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cachePuts;
    private final Counter cacheDeletes;

    public RedisCacheService(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        this.cacheHits = Counter.builder("redis.cache.hits")
                .description("Number of Redis cache hits")
                .tag("cache", "redis-manual")
                .register(meterRegistry);
        this.cacheMisses = Counter.builder("redis.cache.misses")
                .description("Number of Redis cache misses")
                .tag("cache", "redis-manual")
                .register(meterRegistry);
        this.cachePuts = Counter.builder("redis.cache.puts")
                .description("Number of Redis cache puts/sets")
                .tag("cache", "redis-manual")
                .register(meterRegistry);
        this.cacheDeletes = Counter.builder("redis.cache.deletes")
                .description("Number of Redis cache deletes")
                .tag("cache", "redis-manual")
                .register(meterRegistry);
    }

    // ==================== String Operations ====================

    /**
     * Set a value with no expiration.
     */
    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            cachePuts.increment();
            log.debug("Redis SET: {}", key);
        } catch (Exception e) {
            log.error("Redis SET failed for key: {}", key, e);
        }
    }

    /**
     * Set a value with expiration duration.
     */
    public void set(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            cachePuts.increment();
            log.debug("Redis SET: {} (TTL: {})", key, ttl);
        } catch (Exception e) {
            log.error("Redis SET failed for key: {}", key, e);
        }
    }

    /**
     * Set a value with expiration in seconds.
     */
    public void set(String key, String value, long ttlSeconds) {
        set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    /**
     * Get a value by key.
     */
    public Optional<String> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                cacheHits.increment();
            } else {
                cacheMisses.increment();
            }
            log.debug("Redis GET: {} -> {}", key, value != null ? "HIT" : "MISS");
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Redis GET failed for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Get a value or return default if not found.
     */
    public String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * Delete a key.
     */
    public boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(result)) {
                cacheDeletes.increment();
            }
            log.debug("Redis DELETE: {} -> {}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis DELETE failed for key: {}", key, e);
            return false;
        }
    }

    /**
     * Delete multiple keys.
     */
    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            if (count != null && count > 0) {
                cacheDeletes.increment(count);
            }
            log.debug("Redis DELETE: {} keys -> {} deleted", keys.size(), count);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis DELETE failed for keys: {}", keys, e);
            return 0;
        }
    }

    /**
     * Delete all keys matching a pattern.
     * Example: deleteByPattern("user:*") deletes all user keys.
     */
    public long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long count = redisTemplate.delete(keys);
                log.debug("Redis DELETE pattern: {} -> {} deleted", pattern, count);
                return count != null ? count : 0;
            }
            return 0;
        } catch (Exception e) {
            log.error("Redis DELETE pattern failed: {}", pattern, e);
            return 0;
        }
    }

    /**
     * Check if a key exists.
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Redis EXISTS check failed for key: {}", key, e);
            return false;
        }
    }

    /**
     * Set expiration on an existing key.
     */
    public boolean expire(String key, Duration ttl) {
        try {
            Boolean result = redisTemplate.expire(key, ttl);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis EXPIRE failed for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get remaining TTL for a key.
     */
    public Optional<Long> getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl >= 0 ? Optional.of(ttl) : Optional.empty();
        } catch (Exception e) {
            log.error("Redis TTL check failed for key: {}", key, e);
            return Optional.empty();
        }
    }

    // ==================== Object Operations (JSON) ====================

    /**
     * Set an object (serialized to JSON).
     */
    public <T> void setObject(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("Redis SET object serialization failed for key: {}", key, e);
        }
    }

    /**
     * Set an object with no expiration.
     */
    public <T> void setObject(String key, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json);
        } catch (JsonProcessingException e) {
            log.error("Redis SET object serialization failed for key: {}", key, e);
        }
    }

    /**
     * Get an object (deserialized from JSON).
     */
    public <T> Optional<T> getObject(String key, Class<T> clazz) {
        Optional<String> cached = get(key);
        log.debug("Redis GET object: {} -> {}", key, cached.orElse(null));
        
        if (cached.isEmpty()) {
            log.debug("Redis GET object: {} -> MISS", key);
            return Optional.empty();
        }
        
        try {
            log.debug("Redis GET object: {} -> HIT", key);
            return Optional.of(objectMapper.readValue(cached.get(), clazz));
        } catch (JsonProcessingException e) {
            log.error("Redis GET object deserialization failed for key: {}", key, e);
            return Optional.empty();
        }
    }

    // ==================== Hash Operations ====================

    /**
     * Set a hash field.
     */
    public void hashSet(String key, String field, String value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("Redis HSET: {}:{}", key, field);
        } catch (Exception e) {
            log.error("Redis HSET failed for key: {}:{}", key, field, e);
        }
    }

    /**
     * Set multiple hash fields.
     */
    public void hashSetAll(String key, Map<String, String> fields) {
        try {
            redisTemplate.opsForHash().putAll(key, fields);
            log.debug("Redis HMSET: {} ({} fields)", key, fields.size());
        } catch (Exception e) {
            log.error("Redis HMSET failed for key: {}", key, e);
        }
    }

    /**
     * Get a hash field.
     */
    public Optional<String> hashGet(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            return Optional.ofNullable(value).map(Object::toString);
        } catch (Exception e) {
            log.error("Redis HGET failed for key: {}:{}", key, field, e);
            return Optional.empty();
        }
    }

    /**
     * Get all hash fields.
     */
    public Map<String, String> hashGetAll(String key) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            return entries.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> e.getValue().toString()
                    ));
        } catch (Exception e) {
            log.error("Redis HGETALL failed for key: {}", key, e);
            return Map.of();
        }
    }

    /**
     * Delete hash fields.
     */
    public long hashDelete(String key, String... fields) {
        try {
            Long count = redisTemplate.opsForHash().delete(key, (Object[]) fields);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis HDEL failed for key: {}", key, e);
            return 0;
        }
    }

    // ==================== List Operations ====================

    /**
     * Push value to the right of a list.
     */
    public long listPush(String key, String value) {
        try {
            Long size = redisTemplate.opsForList().rightPush(key, value);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Redis RPUSH failed for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Get list range.
     */
    public List<String> listRange(String key, long start, long end) {
        try {
            List<String> result = redisTemplate.opsForList().range(key, start, end);
            return result != null ? result : List.of();
        } catch (Exception e) {
            log.error("Redis LRANGE failed for key: {}", key, e);
            return List.of();
        }
    }

    /**
     * Get list size.
     */
    public long listSize(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Redis LLEN failed for key: {}", key, e);
            return 0;
        }
    }

    // ==================== Set Operations ====================

    /**
     * Add value to a set.
     */
    public long setAdd(String key, String... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis SADD failed for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Get all members of a set.
     */
    public Set<String> setMembers(String key) {
        try {
            Set<String> members = redisTemplate.opsForSet().members(key);
            return members != null ? members : Set.of();
        } catch (Exception e) {
            log.error("Redis SMEMBERS failed for key: {}", key, e);
            return Set.of();
        }
    }

    /**
     * Check if value is member of set.
     */
    public boolean setIsMember(String key, String value) {
        try {
            Boolean result = redisTemplate.opsForSet().isMember(key, value);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis SISMEMBER failed for key: {}", key, e);
            return false;
        }
    }

    /**
     * Remove value from set.
     */
    public long setRemove(String key, String... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, (Object[]) values);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis SREM failed for key: {}", key, e);
            return 0;
        }
    }

    // ==================== Counter Operations ====================

    /**
     * Increment a counter.
     */
    public long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Redis INCR failed for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Increment by amount.
     */
    public long incrementBy(String key, long delta) {
        try {
            Long value = redisTemplate.opsForValue().increment(key, delta);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Redis INCRBY failed for key: {}", key, e);
            return 0;
        }
    }

    /**
     * Decrement a counter.
     */
    public long decrement(String key) {
        try {
            Long value = redisTemplate.opsForValue().decrement(key);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Redis DECR failed for key: {}", key, e);
            return 0;
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Set value only if key doesn't exist (SETNX).
     * Returns true if set, false if key already exists.
     */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis SETNX failed for key: {}", key, e);
            return false;
        }
    }

    /**
     * Get all keys matching a pattern.
     */
    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            return keys != null ? keys : Set.of();
        } catch (Exception e) {
            log.error("Redis KEYS failed for pattern: {}", pattern, e);
            return Set.of();
        }
    }
}
