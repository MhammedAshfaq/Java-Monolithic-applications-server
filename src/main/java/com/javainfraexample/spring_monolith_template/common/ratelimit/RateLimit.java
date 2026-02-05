package com.javainfraexample.spring_monolith_template.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply specific rate limiting to a controller method.
 * 
 * Usage:
 *   @RateLimit(type = RateLimitType.STRICT)
 *   @PostMapping("/login")
 *   public ResponseEntity<...> login(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Rate limit type to apply.
     */
    RateLimitType type() default RateLimitType.SHORT_TERM;
    
    /**
     * Optional custom key suffix (appended to IP).
     * Useful for endpoint-specific limits.
     */
    String key() default "";
}
