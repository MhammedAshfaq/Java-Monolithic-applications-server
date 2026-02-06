package com.javainfraexample.spring_monolith_template.common.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level Redis caching.
 * 
 * Usage:
 * <pre>
 * @RedisCached(key = "user", ttlSeconds = 3600)
 * public User getUser(String userId) {
 *     // This result will be cached for 1 hour
 *     return userRepository.findById(userId);
 * }
 * 
 * @RedisCached(key = "user:profile", ttlSeconds = 1800)
 * public UserProfile getProfile(String userId) {
 *     return profileService.getProfile(userId);
 * }
 * </pre>
 * 
 * The cache key is built as: {key}:{method arguments}
 * Example: @RedisCached(key = "user") with userId "123" -> "user:123"
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCached {
    
    /**
     * Cache key prefix.
     */
    String key();
    
    /**
     * Time to live in seconds. Default: 3600 (1 hour)
     */
    long ttlSeconds() default 3600;
    
    /**
     * Whether to cache null values. Default: false
     */
    boolean cacheNull() default false;
}
