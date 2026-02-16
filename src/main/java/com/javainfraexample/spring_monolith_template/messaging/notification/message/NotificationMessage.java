package com.javainfraexample.spring_monolith_template.messaging.notification.message;

import java.util.List;
import java.util.Map;

/**
 * Unified notification message payload — used across all 3 notification types.
 *
 * <p>The {@code type} field determines which queue it is routed to and how
 * the listener processes it.</p>
 *
 * <h3>Types:</h3>
 * <ul>
 *   <li><b>SINGLE</b> — {@code token} is set, delivered to one device/user</li>
 *   <li><b>MULTICAST</b> — {@code tokens} is set, delivered to multiple devices/users</li>
 *   <li><b>TOPIC</b> — {@code topic} is set, broadcast to all subscribers</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>
 * NotificationMessage.single("device-token-123", "Welcome!", "Thanks for signing up.", "HIGH", Map.of("url", "/home"));
 * NotificationMessage.multicast(List.of("token1", "token2"), "Update", "Sprint review at 3pm", "NORMAL", Map.of());
 * NotificationMessage.topic("product-updates", "New Feature!", "Dark mode is here", "LOW", Map.of("version", "2.0"));
 * </pre>
 *
 * @param type     notification type: SINGLE, MULTICAST, TOPIC
 * @param token    target device/user token (for SINGLE)
 * @param tokens   list of target tokens (for MULTICAST)
 * @param topic    topic name (for TOPIC)
 * @param title    notification title
 * @param body     notification body text
 * @param priority priority level: HIGH, NORMAL, LOW
 * @param data     additional payload (e.g. deep link URL, image URL, action)
 */
public record NotificationMessage(
        String type,
        String token,
        List<String> tokens,
        String topic,
        String title,
        String body,
        String priority,
        Map<String, Object> data
) {

    /** Create a single (one-to-one) notification. */
    public static NotificationMessage single(String token, String title, String body, String priority, Map<String, Object> data) {
        return new NotificationMessage("SINGLE", token, null, null, title, body, priority, data);
    }

    /** Create a multicast (one-to-many) notification. */
    public static NotificationMessage multicast(List<String> tokens, String title, String body, String priority, Map<String, Object> data) {
        return new NotificationMessage("MULTICAST", null, tokens, null, title, body, priority, data);
    }

    /** Create a topic (broadcast) notification. */
    public static NotificationMessage topic(String topic, String title, String body, String priority, Map<String, Object> data) {
        return new NotificationMessage("TOPIC", null, null, topic, title, body, priority, data);
    }
}
