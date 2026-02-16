package com.javainfraexample.spring_monolith_template.messaging.notification.publisher;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;
import com.javainfraexample.spring_monolith_template.messaging.notification.constant.NotificationConstants;
import com.javainfraexample.spring_monolith_template.messaging.notification.message.NotificationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Single publisher for all notification types — routes to the correct queue based on type.
 *
 * <h3>Usage:</h3>
 * <pre>
 * notificationPublisher.sendSingle("token-123", "Welcome!", "Thanks for signing up.", "HIGH", Map.of("url", "/home"));
 * notificationPublisher.sendMulticast(List.of("t1", "t2"), "Update", "Sprint review", "NORMAL", Map.of());
 * notificationPublisher.sendTopic("product-updates", "New Feature!", "Dark mode", "LOW", Map.of("version", "2.0"));
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send a notification message — automatically routes to the correct queue based on {@code type}.
     */
    public void send(NotificationMessage message) {
        String routingKey = resolveRoutingKey(message.type());
        rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, routingKey, message);
        log.info("Notification published: type={}, priority={}, routingKey={}", message.type(), message.priority(), routingKey);
    }

    /** Send a single (one-to-one) notification. */
    public void sendSingle(String token, String title, String body, String priority, Map<String, Object> data) {
        send(NotificationMessage.single(token, title, body, priority, data));
    }

    /** Send a multicast (one-to-many) notification. */
    public void sendMulticast(List<String> tokens, String title, String body, String priority, Map<String, Object> data) {
        send(NotificationMessage.multicast(tokens, title, body, priority, data));
    }

    /** Send a topic (broadcast) notification. */
    public void sendTopic(String topic, String title, String body, String priority, Map<String, Object> data) {
        send(NotificationMessage.topic(topic, title, body, priority, data));
    }

    private String resolveRoutingKey(String type) {
        return switch (type) {
            case NotificationConstants.TYPE_SINGLE    -> QueueConstants.NOTIFICATION_SINGLE_ROUTING_KEY;
            case NotificationConstants.TYPE_MULTICAST -> QueueConstants.NOTIFICATION_MULTICAST_ROUTING_KEY;
            case NotificationConstants.TYPE_TOPIC     -> QueueConstants.NOTIFICATION_TOPIC_ROUTING_KEY;
            default -> throw new IllegalArgumentException("Unknown notification type: " + type);
        };
    }
}
