package com.javainfraexample.spring_monolith_template.messaging.notification.listener;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes topic-based notification messages — broadcasts to all subscribers of a topic.
 *
 * <p>Retry: Spring auto-retry (3 attempts, exponential backoff) → DLQ on exhaustion.</p>
 * <p>Queue: {@code app.notification.topic}</p>
 * <p>DLQ: {@code app.notification.topic.dlq}</p>
 */
@Slf4j
@Component
public class TopicNotificationListener {

    // TODO: Inject your services here
    // private final NotificationService notificationService;

    @RabbitListener(queues = QueueConstants.NOTIFICATION_TOPIC_QUEUE)
    public void onMessage(String payload) {
        log.info("Topic notification received: {}", payload);

        // TODO: Deserialize and broadcast to topic subscribers
        // NotificationMessage notif = objectMapper.readValue(payload, NotificationMessage.class);
        // notificationService.sendToTopic(notif.topic(), notif.title(), notif.body(), notif.priority(), notif.data());
    }
}
