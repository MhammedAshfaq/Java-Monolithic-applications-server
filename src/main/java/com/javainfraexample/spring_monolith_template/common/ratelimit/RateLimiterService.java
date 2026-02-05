package com.javainfraexample.spring_monolith_template.common.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * IP-based rate limiter using Redis sliding window counter.
 * 
 * Usage:
 *   RateLimitResult result = rateLimiterService.checkLimit("192.168.1.1", RateLimitType.SHORT_TERM);
 *   if (!result.allowed()) {
 *       // Return 429 Too Many Requests
 *   }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {
    
    private static final String KEY_PREFIX = "rate_limit:";
    
    private final StringRedisTemplate redisTemplate;
    private final RateLimitConfig config;
    
    /**
     * Check if request is allowed for given IP and rate limit type.
     * 
     * @param ip Client IP address
     * @param type Rate limit type (SHORT_TERM, LONG_TERM, STRICT)
     * @return RateLimitResult with allowed status and remaining requests
     */
    public RateLimitResult checkLimit(String ip, RateLimitType type) {
        if (!config.isEnabled()) {
            return RateLimitResult.allowed(-1, -1);
        }
        
        RateLimitConfig.Limit limit = config.getLimit(type);
        String key = buildKey(ip, type);
        
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            
            if (currentCount == null) {
                log.warn("Redis increment returned null for key: {}", key);
                return RateLimitResult.allowed(-1, -1); // Fail open
            }
            
            // Set expiry on first request
            if (currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(limit.getWindowSeconds()));
            }
            
            int remaining = Math.max(0, limit.getMaxRequests() - currentCount.intValue());
            long resetSeconds = getResetSeconds(key, limit.getWindowSeconds());
            
            if (currentCount > limit.getMaxRequests()) {
                log.debug("Rate limit exceeded for IP: {}, type: {}, count: {}", ip, type, currentCount);
                return RateLimitResult.exceeded(remaining, resetSeconds);
            }
            
            return RateLimitResult.allowed(remaining, resetSeconds);
            
        } catch (Exception e) {
            log.error("Rate limit check failed for IP: {}, type: {}", ip, type, e);
            return RateLimitResult.allowed(-1, -1); // Fail open on Redis errors
        }
    }
    
    /**
     * Check multiple rate limit types. Returns first exceeded limit or allowed.
     */
    public RateLimitResult checkLimits(String ip, RateLimitType... types) {
        for (RateLimitType type : types) {
            RateLimitResult result = checkLimit(ip, type);
            if (!result.allowed()) {
                return result;
            }
        }
        return RateLimitResult.allowed(-1, -1);
    }
    
    /**
     * Get current request count for IP and type.
     */
    public Optional<Integer> getCurrentCount(String ip, RateLimitType type) {
        String key = buildKey(ip, type);
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Optional.of(Integer.parseInt(value)) : Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get rate limit count for IP: {}", ip, e);
            return Optional.empty();
        }
    }
    
    /**
     * Reset rate limit for IP and type (admin use).
     */
    public void resetLimit(String ip, RateLimitType type) {
        String key = buildKey(ip, type);
        try {
            redisTemplate.delete(key);
            log.info("Rate limit reset for IP: {}, type: {}", ip, type);
        } catch (Exception e) {
            log.error("Failed to reset rate limit for IP: {}", ip, e);
        }
    }
    
    /**
     * Reset all rate limits for IP (admin use).
     */
    public void resetAllLimits(String ip) {
        for (RateLimitType type : RateLimitType.values()) {
            resetLimit(ip, type);
        }
    }
    
    private String buildKey(String ip, RateLimitType type) {
        return KEY_PREFIX + type.name().toLowerCase() + ":" + ip;
    }
    
    private long getResetSeconds(String key, int windowSeconds) {
        try {
            Long ttl = redisTemplate.getExpire(key);
            return ttl != null && ttl > 0 ? ttl : windowSeconds;
        } catch (Exception e) {
            return windowSeconds;
        }
    }
    
    /**
     * Result of rate limit check.
     */
    public record RateLimitResult(
        boolean allowed,
        int remaining,
        long resetSeconds
    ) {
        public static RateLimitResult allowed(int remaining, long resetSeconds) {
            return new RateLimitResult(true, remaining, resetSeconds);
        }
        
        public static RateLimitResult exceeded(int remaining, long resetSeconds) {
            return new RateLimitResult(false, remaining, resetSeconds);
        }
    }
}
