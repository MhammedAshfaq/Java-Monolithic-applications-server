package com.javainfraexample.spring_monolith_template.messaging.dlq;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Shared DLQ listener — consumes messages from ALL dead letter queues.
 *
 * <p>When any message fails 3 retries and lands in a DLQ, this listener picks it up,
 * identifies the source, and triggers a Slack alert.</p>
 *
 * <h3>Flow:</h3>
 * <pre>
 *   app.email.send.dlq              ─┐
 *   app.notification.single.dlq     ─┤
 *   app.notification.multicast.dlq  ─┼──→ DlqListener → DlqNotificationService
 *   app.notification.topic.dlq      ─┤        ├── Identify source
 *   app.audit.event.dlq             ─┘        ├── Emit Slack alert
 *                                              └── Store in DB (TODO)
 * </pre>
 *
 * <h3>Important:</h3>
 * <p>This listener does NOT have retry enabled — if DLQ processing itself fails,
 * we log the error but don't re-queue (to avoid infinite loops).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqListener {

    private final DlqNotificationService dlqNotificationService;

    @RabbitListener(queues = QueueConstants.EMAIL_DLQ)
    public void onEmailDlq(Message message) {
        processSafely(message, QueueConstants.EMAIL_DLQ);
    }

    @RabbitListener(queues = QueueConstants.NOTIFICATION_SINGLE_DLQ)
    public void onNotificationSingleDlq(Message message) {
        processSafely(message, QueueConstants.NOTIFICATION_SINGLE_DLQ);
    }

    @RabbitListener(queues = QueueConstants.NOTIFICATION_MULTICAST_DLQ)
    public void onNotificationMulticastDlq(Message message) {
        processSafely(message, QueueConstants.NOTIFICATION_MULTICAST_DLQ);
    }

    @RabbitListener(queues = QueueConstants.NOTIFICATION_TOPIC_DLQ)
    public void onNotificationTopicDlq(Message message) {
        processSafely(message, QueueConstants.NOTIFICATION_TOPIC_DLQ);
    }

    @RabbitListener(queues = QueueConstants.AUDIT_DLQ)
    public void onAuditDlq(Message message) {
        processSafely(message, QueueConstants.AUDIT_DLQ);
    }

    /**
     * Process DLQ message safely — never throw to avoid infinite loop.
     */
    private void processSafely(Message message, String dlqName) {
        try {
            dlqNotificationService.process(message, dlqName);
        } catch (Exception e) {
            log.error("DLQ processing itself failed for queue={}: {}", dlqName, e.getMessage(), e);
        }
    }
}
