package com.javainfraexample.spring_monolith_template.messaging.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Audit event payload — sent to the audit queue, consumed by {@code AuditListener}.
 *
 * @param action    what happened (e.g. USER_REGISTERED, PASSWORD_CHANGED, ORDER_CREATED)
 * @param userId    who performed the action (null for system events)
 * @param ip        client IP address (null if not applicable)
 * @param details   additional context (flexible key-value pairs)
 * @param timestamp when the event occurred
 */
public record AuditMessage(
        String action,
        String userId,
        String ip,
        Map<String, Object> details,
        Instant timestamp
) {
    /** Convenience constructor with auto-timestamp. */
    public AuditMessage(String action, String userId, Map<String, Object> details) {
        this(action, userId, null, details, Instant.now());
    }
}
