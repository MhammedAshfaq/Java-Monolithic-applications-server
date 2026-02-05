package com.javainfraexample.spring_monolith_template.common.ratelimit;

/**
 * Rate limit types with different time windows.
 */
public enum RateLimitType {
    
    /**
     * Short-term: High requests allowed in short window (e.g., 100 req/min)
     * Use for: Normal API endpoints, general browsing
     */
    SHORT_TERM,
    
    /**
     * Long-term: Lower requests over longer window (e.g., 1000 req/hour)  
     * Use for: Preventing abuse, overall usage caps
     */
    LONG_TERM,
    
    /**
     * Strict: Very limited requests (e.g., 5 req/min)
     * Use for: Login attempts, password reset, sensitive operations
     */
    STRICT
}
