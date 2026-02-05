package com.javainfraexample.spring_monolith_template.config.db;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing Configuration.
 * 
 * Enables automatic population of audit fields:
 * - @CreatedDate: auto-fills when entity is created
 * - @LastModifiedDate: auto-fills when entity is updated
 * - @CreatedBy: auto-fills with current user (requires AuditorAware)
 * - @LastModifiedBy: auto-fills with current user (requires AuditorAware)
 * 
 * Usage in Entity:
 * <pre>
 * @Entity
 * @EntityListeners(AuditingEntityListener.class)
 * public class User {
 *     @CreatedDate
 *     private LocalDateTime createdAt;
 *     
 *     @LastModifiedDate
 *     private LocalDateTime updatedAt;
 * }
 * </pre>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    
    // Optional: Add AuditorAware bean if you need @CreatedBy/@LastModifiedBy
    // @Bean
    // public AuditorAware<String> auditorProvider() {
    //     return () -> Optional.ofNullable(SecurityContextHolder.getContext())
    //             .map(SecurityContext::getAuthentication)
    //             .filter(Authentication::isAuthenticated)
    //             .map(Authentication::getName);
    // }
}
