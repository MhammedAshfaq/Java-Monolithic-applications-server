package com.javainfraexample.spring_monolith_template.common.redis;

/**
 * Redis key prefixes for organizing keys.
 * 
 * Usage:
 * <pre>
 * String key = RedisKey.USER.key(userId);  // "user:123"
 * String key = RedisKey.SESSION.key(sessionId);  // "session:abc123"
 * </pre>
 */
public enum RedisKey {
    
    // User related
    USER("user"),
    USER_SESSION("user:session"),
    USER_PROFILE("user:profile"),
    
    // Auth related
    AUTH_TOKEN("auth:token"),
    REFRESH_TOKEN("refresh:token"),
    PASSWORD_RESET("password:reset"),
    EMAIL_VERIFICATION("email:verify"),
    
    // Rate limiting
    RATE_LIMIT("rate:limit"),
    
    // General cache
    CACHE("cache"),
    
    // Temporary data
    TEMP("temp"),
    OTP("otp");
    
    private final String prefix;
    
    RedisKey(String prefix) {
        this.prefix = prefix;
    }
    
    /**
     * Get the prefix string.
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Build a full key with the given identifier.
     * Example: RedisKey.USER.key("123") returns "user:123"
     */
    public String key(String identifier) {
        return prefix + ":" + identifier;
    }
    
    /**
     * Build a full key with multiple parts.
     * Example: RedisKey.USER.key("123", "profile") returns "user:123:profile"
     */
    public String key(String... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        for (String part : parts) {
            sb.append(":").append(part);
        }
        return sb.toString();
    }
}
