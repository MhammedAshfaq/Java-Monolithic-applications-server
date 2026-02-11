package com.javainfraexample.spring_monolith_template.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for the application.
 * Provides RedisTemplate beans for string operations (rate limiting, counters, caching, etc.)
 *
 * <p>Two caching mechanisms are available:</p>
 * <ul>
 *   <li><b>Spring Cache (@Cacheable)</b> — Uses RedisCacheManager, auto-instrumented by Micrometer
 *       (metrics: cache_gets_total, cache_puts_total, cache_removals_total visible in Grafana)</li>
 *   <li><b>RedisCacheService</b> — Manual Redis operations, instrumented with custom Micrometer counters
 *       (metrics: redis_cache_hits_total, redis_cache_misses_total, redis_cache_puts_total)</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * ObjectMapper for JSON serialization in cache operations.
     * Configured to handle Java 8 date/time types.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * RedisCacheManager for Spring's @Cacheable / @CacheEvict / @CachePut annotations.
     * Micrometer auto-instruments this CacheManager — metrics appear in Grafana automatically.
     *
     * Default TTL: 1 hour. Override per-cache:
     * <pre>
     *   @Cacheable(value = "users", key = "#id")
     *   public User findById(UUID id) { ... }
     * </pre>
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Default TTL for all caches (override per-cache below if needed)
                .entryTtl(Duration.ofHours(1))
                // Prefix cache keys: e.g. "cache:users::uuid-123"
                .prefixCacheNameWith("cache:")
                // Serialize keys as strings, values as JSON
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(RedisSerializer.json()))
                // Don't cache null values
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // Register named caches with custom TTLs if needed:
                // .withCacheConfiguration("sessions", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                // .withCacheConfiguration("products", defaultConfig.entryTtl(Duration.ofHours(6)))
                .enableStatistics()
                .build();
    }

    /**
     * Primary StringRedisTemplate for all string-based Redis operations.
     * Used by RateLimiterService and other services.
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Generic RedisTemplate for Object storage (JSON serialization).
     * Use StringRedisTemplate for simple operations like counters.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
