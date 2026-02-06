package com.javainfraexample.spring_monolith_template.common.redis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to evict Redis cache when a method is called.
 * 
 * Usage:
 * <pre>
 * @RedisCacheEvict(key = "user")
 * public void updateUser(String userId, UpdateRequest request) {
 *     // Cache for "user:{userId}" will be deleted after this method
 *     userRepository.update(userId, request);
 * }
 * 
 * @RedisCacheEvict(key = "user", allEntries = true)
 * public void clearAllUsers() {
 *     // All keys matching "user:*" will be deleted
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCacheEvict {
    
    /**
     * Cache key prefix to evict.
     */
    String key();
    
    /**
     * If true, evict all entries with this key prefix.
     * Default: false (only evict specific key based on arguments)
     */
    boolean allEntries() default false;
    
    /**
     * If true, evict before method execution.
     * Default: false (evict after method execution)
     */
    boolean beforeInvocation() default false;
}
