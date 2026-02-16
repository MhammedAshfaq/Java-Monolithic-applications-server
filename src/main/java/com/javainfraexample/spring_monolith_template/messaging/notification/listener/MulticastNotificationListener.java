package com.javainfraexample.spring_monolith_template.messaging.notification.listener;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes multicast notification messages — delivers to a list of tokens.
 *
 * <p>Retry: Spring auto-retry (3 attempts, exponential backoff) → DLQ on exhaustion.</p>
 * <p>Queue: {@code app.notification.multicast}</p>
 * <p>DLQ: {@code app.notification.multicast.dlq}</p>
 */
@Slf4j
@Component
public class MulticastNotificationListener {

    // TODO: Inject your notification service here
    // private final NotificationService notificationService;

    @RabbitListener(queues = QueueConstants.NOTIFICATION_MULTICAST_QUEUE)
    public void onMessage(String payload) {
        log.info("Multicast notification received: {}", payload);

        // TODO: Deserialize and fan out to each token
        // NotificationMessage notif = objectMapper.readValue(payload, NotificationMessage.class);
        // notificationService.sendToTokens(notif.tokens(), notif.title(), notif.body(), notif.priority(), notif.data());
    }
}
