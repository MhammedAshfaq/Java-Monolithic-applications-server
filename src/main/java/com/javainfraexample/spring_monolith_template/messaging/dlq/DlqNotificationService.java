package com.javainfraexample.spring_monolith_template.messaging.dlq;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes dead-lettered messages — builds a {@link DlqMessage}, identifies the source,
 * emits Slack alert, and (future) stores in DB.
 *
 * <h3>Flow:</h3>
 * <pre>
 *   DlqListener → DlqNotificationService
 *                    ├── Identify source (email / notification / audit)
 *                    ├── Build DlqMessage with error details
 *                    ├── Send Slack alert
 *                    └── Store in DB (TODO)
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqNotificationService {

    private final DlqSlackNotifier slackNotifier;

    // TODO: Inject repository to persist DLQ records
    // private final DlqRecordRepository dlqRecordRepository;

    /**
     * Process a dead-lettered message — extract info, alert, and persist.
     */
    public void process(Message message, String dlqName) {
        DlqMessage dlqMessage = buildDlqMessage(message, dlqName);

        log.error("DLQ message received: queue={}, type={}, retries={}, error={}",
                dlqMessage.originalQueue(),
                dlqMessage.messageType(),
                dlqMessage.retryCount(),
                dlqMessage.errorReason());

        // 1. Send Slack alert
        slackNotifier.notify(dlqMessage);

        // 2. Store in DB (commented — implement when ready)
        // dlqRecordRepository.save(DlqRecord.from(dlqMessage));
    }

    /**
     * Build a DlqMessage from the raw RabbitMQ message + x-death headers.
     */
    private DlqMessage buildDlqMessage(Message message, String dlqName) {
        MessageProperties props = message.getMessageProperties();
        String payload = new String(message.getBody());

        // Extract original queue from x-death header
        String originalQueue = extractOriginalQueue(props);
        String messageType = resolveMessageType(originalQueue);
        int retryCount = extractRetryCount(props);
        String errorReason = extractErrorReason(props);

        // Collect useful headers for debugging
        Map<String, Object> headers = new HashMap<>();
        if (props.getHeaders() != null) {
            props.getHeaders().forEach((k, v) -> headers.put(k, v != null ? v.toString() : null));
        }

        return new DlqMessage(
                originalQueue != null ? originalQueue : dlqName,
                messageType,
                retryCount,
                errorReason,
                payload,
                headers,
                Instant.now()
        );
    }

    /**
     * Identify the message type based on the original queue name.
     */
    private String resolveMessageType(String originalQueue) {
        if (originalQueue == null) return "UNKNOWN";

        return switch (originalQueue) {
            case QueueConstants.EMAIL_QUEUE -> "EMAIL";
            case QueueConstants.NOTIFICATION_SINGLE_QUEUE -> "SINGLE_NOTIFICATION";
            case QueueConstants.NOTIFICATION_MULTICAST_QUEUE -> "MULTICAST_NOTIFICATION";
            case QueueConstants.NOTIFICATION_TOPIC_QUEUE -> "TOPIC_NOTIFICATION";
            case QueueConstants.AUDIT_QUEUE -> "AUDIT";
            default -> "UNKNOWN (" + originalQueue + ")";
        };
    }

    /**
     * Extract original queue name from the x-death header.
     */
    @SuppressWarnings("unchecked")
    private String extractOriginalQueue(MessageProperties props) {
        List<Map<String, Object>> xDeath = (List<Map<String, Object>>) props.getHeader("x-death");
        if (xDeath != null && !xDeath.isEmpty()) {
            Object queue = xDeath.getFirst().get("queue");
            if (queue != null) return queue.toString();
        }
        return null;
    }

    /**
     * Extract retry count from the x-death header.
     */
    @SuppressWarnings("unchecked")
    private int extractRetryCount(MessageProperties props) {
        List<Map<String, Object>> xDeath = (List<Map<String, Object>>) props.getHeader("x-death");
        if (xDeath != null && !xDeath.isEmpty()) {
            Object count = xDeath.getFirst().get("count");
            if (count instanceof Number) return ((Number) count).intValue();
        }
        return 0;
    }

    /**
     * Extract error reason from headers (Spring sets the exception in headers).
     */
    private String extractErrorReason(MessageProperties props) {
        // Spring AMQP stores the exception in x-exception-message header
        Object exceptionMsg = props.getHeader("x-exception-message");
        if (exceptionMsg != null) return exceptionMsg.toString();

        // Fall back to x-death reason
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> xDeath = (List<Map<String, Object>>) props.getHeader("x-death");
        if (xDeath != null && !xDeath.isEmpty()) {
            Object reason = xDeath.getFirst().get("reason");
            if (reason != null) return reason.toString();
        }

        return "unknown";
    }
}
