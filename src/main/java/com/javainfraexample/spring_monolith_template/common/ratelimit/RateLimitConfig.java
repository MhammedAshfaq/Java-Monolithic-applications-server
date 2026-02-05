package com.javainfraexample.spring_monolith_template.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Rate limit configuration loaded from application.yaml.
 * 
 * Example configuration:
 * app:
 *   rate-limit:
 *     enabled: true
 *     short-term:
 *       max-requests: 100
 *       window-seconds: 60
 *     long-term:
 *       max-requests: 1000
 *       window-seconds: 3600
 *     strict:
 *       max-requests: 5
 *       window-seconds: 60
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {
    
    private boolean enabled = true;
    
    private Limit shortTerm = new Limit(100, 60);      // 100 req/min
    private Limit longTerm = new Limit(1000, 3600);    // 1000 req/hour
    private Limit strict = new Limit(5, 60);           // 5 req/min
    
    @Getter
    @Setter
    public static class Limit {
        private int maxRequests;
        private int windowSeconds;
        
        public Limit() {}
        
        public Limit(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
        }
    }
    
    public Limit getLimit(RateLimitType type) {
        return switch (type) {
            case SHORT_TERM -> shortTerm;
            case LONG_TERM -> longTerm;
            case STRICT -> strict;
        };
    }
}
