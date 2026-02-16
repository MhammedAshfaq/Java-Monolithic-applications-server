package com.javainfraexample.spring_monolith_template.messaging.constant;

/**
 * Central registry of all RabbitMQ exchange, queue, and routing key names.
 * Avoids hardcoded strings scattered across publishers and listeners.
 *
 * <h3>Naming convention:</h3>
 * <pre>
 *   Exchange:    app.exchange
 *   Queue:       app.{domain}.{action}
 *   Routing key: app.{domain}.{action}
 *   DLQ:         app.{domain}.{action}.dlq
 * </pre>
 *
 * <h3>How to add a new queue:</h3>
 * <ol>
 *   <li>Add constants here (queue, routing key, DLQ)</li>
 *   <li>Create a new domain folder under {@code messaging/}</li>
 *   <li>Add {@code XxxQueueConfig.java} — queue, DLQ, bindings (self-contained)</li>
 *   <li>Add {@code XxxMessage.java} — DTO record</li>
 *   <li>Add {@code XxxPublisher.java} — sends messages</li>
 *   <li>Add {@code XxxListener.java} — consumes messages</li>
 * </ol>
 */
public final class QueueConstants {

    private QueueConstants() {}

    // ===========================================
    // Exchanges (shared)
    // ===========================================

    /** Main topic exchange — routes all messages by routing key pattern. */
    public static final String EXCHANGE = "app.exchange";

    /** Dead letter exchange — receives messages that failed after all retries. */
    public static final String DLX_EXCHANGE = "app.exchange.dlx";

    // ===========================================
    // Email
    // ===========================================

    public static final String EMAIL_QUEUE = "app.email.send";
    public static final String EMAIL_ROUTING_KEY = "app.email.send";
    public static final String EMAIL_DLQ = "app.email.send.dlq";

    // ===========================================
    // Notification — Single (one-to-one)
    // ===========================================

    /** Send a notification to a single specific user. */
    public static final String NOTIFICATION_SINGLE_QUEUE = "app.notification.single";
    public static final String NOTIFICATION_SINGLE_ROUTING_KEY = "app.notification.single";
    public static final String NOTIFICATION_SINGLE_DLQ = "app.notification.single.dlq";

    // ===========================================
    // Notification — Multicast (one-to-many)
    // ===========================================

    /** Send a notification to a specific list of users. */
    public static final String NOTIFICATION_MULTICAST_QUEUE = "app.notification.multicast";
    public static final String NOTIFICATION_MULTICAST_ROUTING_KEY = "app.notification.multicast";
    public static final String NOTIFICATION_MULTICAST_DLQ = "app.notification.multicast.dlq";

    // ===========================================
    // Notification — Topic (broadcast to subscribers)
    // ===========================================

    /** Send a notification to all users subscribed to a topic. */
    public static final String NOTIFICATION_TOPIC_QUEUE = "app.notification.topic";
    public static final String NOTIFICATION_TOPIC_ROUTING_KEY = "app.notification.topic";
    public static final String NOTIFICATION_TOPIC_DLQ = "app.notification.topic.dlq";

    // ===========================================
    // Audit
    // ===========================================

    public static final String AUDIT_QUEUE = "app.audit.event";
    public static final String AUDIT_ROUTING_KEY = "app.audit.event";
    public static final String AUDIT_DLQ = "app.audit.event.dlq";
}
