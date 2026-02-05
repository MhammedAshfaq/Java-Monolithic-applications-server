package com.javainfraexample.spring_monolith_template.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 * HTTP filter that applies rate limiting to all API requests.
 * 
 * Applies both SHORT_TERM and LONG_TERM limits.
 * Returns 429 Too Many Requests when limit is exceeded.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimiterService rateLimiterService;
    private final RateLimitConfig config;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Skip if rate limiting is disabled
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip health/actuator endpoints
        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIp(request);
        
        // Check both short-term and long-term limits
        RateLimiterService.RateLimitResult result = rateLimiterService.checkLimits(
            clientIp,
            RateLimitType.SHORT_TERM,
            RateLimitType.LONG_TERM
        );
        
        // Add rate limit headers
        addRateLimitHeaders(response, result);
        
        if (!result.allowed()) {
            sendRateLimitResponse(response, result, clientIp);
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean shouldSkip(String path) {
        return path.startsWith("/actuator") 
            || path.startsWith("/health")
            || path.equals("/")
            || path.startsWith("/dev")
            || path.startsWith("/apidocs")
            || path.startsWith("/v1/api-docs")
            || path.startsWith("/swagger");
    }
    
    private String getClientIp(HttpServletRequest request) {
        // Check common proxy headers
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    private void addRateLimitHeaders(HttpServletResponse response, RateLimiterService.RateLimitResult result) {
        if (result.remaining() >= 0) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        }
        if (result.resetSeconds() > 0) {
            response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetSeconds()));
        }
    }
    
    private void sendRateLimitResponse(
            HttpServletResponse response,
            RateLimiterService.RateLimitResult result,
            String clientIp
    ) throws IOException {
        log.warn("Rate limit exceeded for IP: {}", clientIp);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(result.resetSeconds()));
        
        String jsonResponse = """
            {
                "timestamp": "%s",
                "status": 429,
                "error": "Too Many Requests",
                "message": "Rate limit exceeded. Please try again in %d seconds.",
                "retryAfter": %d
            }
            """.formatted(LocalDateTime.now().toString(), result.resetSeconds(), result.resetSeconds());
        
        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse);
        writer.flush();
    }
}
