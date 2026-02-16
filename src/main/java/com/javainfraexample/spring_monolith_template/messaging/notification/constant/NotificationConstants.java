package com.javainfraexample.spring_monolith_template.messaging.notification.constant;

/**
 * Notification-specific constants — types and priority levels.
 *
 * <p>Queue/routing key names are in {@code QueueConstants} (shared registry).
 * This class holds domain-specific values used by publishers and listeners.</p>
 */
public final class NotificationConstants {

    private NotificationConstants() {}

    // ===========================================
    // Notification Types (determines which queue)
    // ===========================================

    /** One-to-one: deliver to a single specific user. */
    public static final String TYPE_SINGLE = "SINGLE";

    /** One-to-many: deliver to a specific list of users. */
    public static final String TYPE_MULTICAST = "MULTICAST";

    /** Broadcast: deliver to all subscribers of a topic. */
    public static final String TYPE_TOPIC = "TOPIC";

    // ===========================================
    // Priority Levels
    // ===========================================

    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_LOW = "LOW";
}
