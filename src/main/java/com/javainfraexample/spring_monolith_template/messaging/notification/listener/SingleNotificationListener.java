package com.javainfraexample.spring_monolith_template.messaging.notification.listener;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;
import com.javainfraexample.spring_monolith_template.messaging.notification.message.NotificationMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes single notification messages — delivers to one specific token.
 *
 * <p>Retry: Spring auto-retry (3 attempts, exponential backoff) → DLQ on exhaustion.</p>
 * <p>Queue: {@code app.notification.single}</p>
 * <p>DLQ: {@code app.notification.single.dlq}</p>
 */
@Slf4j
@Component
public class SingleNotificationListener {

    // TODO: Inject your notification service here
    // private final NotificationService notificationService;

    @RabbitListener(queues = QueueConstants.NOTIFICATION_SINGLE_QUEUE)
    public void onMessage(NotificationMessage message) {
        log.info("[NOTIFICATION SINGLE] Received: token={}, title={}, priority={}",
                message.token(), message.title(), message.priority());

        // TODO: Delegate to notification service
        // notificationService.sendToToken(message.token(), message.title(), message.body(), message.priority(), message.data());

        log.info("[NOTIFICATION SINGLE] Processed successfully for token: {}", message.token());
    }
}
