package com.javainfraexample.spring_monolith_template.messaging.notification.listener;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;
import com.javainfraexample.spring_monolith_template.messaging.notification.message.NotificationMessage;

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

    // TODO: Inject your notification service here
    // private final NotificationService notificationService;

    @RabbitListener(queues = QueueConstants.NOTIFICATION_TOPIC_QUEUE)
    public void onMessage(NotificationMessage message) {
        log.info("[NOTIFICATION TOPIC] Received: topic={}, title={}, priority={}",
                message.topic(), message.title(), message.priority());

        // TODO: Delegate to notification service
        // notificationService.sendToTopic(message.topic(), message.title(), message.body(), message.priority(), message.data());

        log.info("[NOTIFICATION TOPIC] Processed successfully for topic: {}", message.topic());
    }
}
