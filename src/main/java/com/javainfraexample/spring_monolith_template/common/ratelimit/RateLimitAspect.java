package com.javainfraexample.spring_monolith_template.common.ratelimit;

import com.javainfraexample.spring_monolith_template.common.ratelimit.RateLimiterService.RateLimitResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Aspect that handles @RateLimit annotation on controller methods.
 * Applies stricter or custom rate limits to specific endpoints.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RateLimiterService rateLimiterService;
    private final RateLimitConfig config;
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        
        if (!config.isEnabled()) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        
        String clientIp = getClientIp(request);
        String key = rateLimit.key().isEmpty() ? clientIp : clientIp + ":" + rateLimit.key();
        
        RateLimitResult result = rateLimiterService.checkLimit(key, rateLimit.type());
        log.info("--------------------------------");
        log.info("[RATE LIMIT ASPECT] RESULT: " + result);
        log.info("--------------------------------");
        
        if (!result.allowed()) {
            log.warn("Rate limit [{}] exceeded for IP: {} on method: {}", 
                rateLimit.type(), clientIp, joinPoint.getSignature().getName());
            
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(result.resetSeconds()))
                .header("X-RateLimit-Remaining", "0")
                .header("X-RateLimit-Reset", String.valueOf(result.resetSeconds()))
                .body(Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded. Try again in " + result.resetSeconds() + " seconds.",
                    "retryAfter", result.resetSeconds()
                ));
        }
        
        return joinPoint.proceed();
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
