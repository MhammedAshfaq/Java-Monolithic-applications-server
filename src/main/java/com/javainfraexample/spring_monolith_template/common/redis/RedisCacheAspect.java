package com.javainfraexample.spring_monolith_template.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Aspect for handling @RedisCached and @RedisCacheEvict annotations.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedisCacheAspect {

    private final RedisCacheService cacheService;

    /**
     * Handle @RedisCached annotation - cache method results.
     */
    @Around("@annotation(redisCached)")
    public Object handleCached(ProceedingJoinPoint joinPoint, RedisCached redisCached) throws Throwable {
        String cacheKey = buildCacheKey(redisCached.key(), joinPoint.getArgs());
        
        // Try to get from cache
        var cachedResult = cacheService.getObject(cacheKey, Object.class);
        if (cachedResult.isPresent()) {
            log.debug("Redis Cache HIT: {}", cacheKey);
            return cachedResult.get();
        }
        
        log.debug("Redis Cache MISS: {}", cacheKey);
        
        // Execute method
        Object result = joinPoint.proceed();
        
        // Cache result (unless null and cacheNull is false)
        if (result != null || redisCached.cacheNull()) {
            cacheService.setObject(cacheKey, result, Duration.ofSeconds(redisCached.ttlSeconds()));
            log.debug("Redis Cache SET: {} (TTL: {}s)", cacheKey, redisCached.ttlSeconds());
        }
        
        return result;
    }

    /**
     * Handle @RedisCacheEvict annotation - evict cache on method call.
     */
    @Around("@annotation(redisCacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, RedisCacheEvict redisCacheEvict) throws Throwable {
        // Evict before if configured
        if (redisCacheEvict.beforeInvocation()) {
            evictCache(redisCacheEvict, joinPoint.getArgs());
        }
        
        // Execute method
        Object result = joinPoint.proceed();
        
        // Evict after (default)
        if (!redisCacheEvict.beforeInvocation()) {
            evictCache(redisCacheEvict, joinPoint.getArgs());
        }
        
        return result;
    }

    private void evictCache(RedisCacheEvict redisCacheEvict, Object[] args) {
        if (redisCacheEvict.allEntries()) {
            // Delete all keys matching pattern
            String pattern = redisCacheEvict.key() + ":*";
            long deleted = cacheService.deleteByPattern(pattern);
            log.debug("Redis Cache EVICT pattern: {} -> {} keys deleted", pattern, deleted);
        } else {
            // Delete specific key
            String cacheKey = buildCacheKey(redisCacheEvict.key(), args);
            cacheService.delete(cacheKey);
            log.debug("Redis Cache EVICT: {}", cacheKey);
        }
    }

    /**
     * Build cache key from prefix and method arguments.
     */
    private String buildCacheKey(String prefix, Object[] args) {
        if (args == null || args.length == 0) {
            return prefix;
        }
        
        String argKey = Arrays.stream(args)
                .map(this::toKeyPart)
                .collect(Collectors.joining(":"));
        
        return prefix + ":" + argKey;
    }

    /**
     * Convert argument to string for cache key.
     */
    private String toKeyPart(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
            return arg.toString();
        }
        // For complex objects, use hashCode
        return String.valueOf(arg.hashCode());
    }
}
