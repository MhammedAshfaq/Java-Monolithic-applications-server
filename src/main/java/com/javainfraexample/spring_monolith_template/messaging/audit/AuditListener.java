package com.javainfraexample.spring_monolith_template.messaging.audit;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the audit queue and delegates to the audit service.
 *
 * <p>Retry: Spring auto-retry (3 attempts, exponential backoff) → DLQ on exhaustion.</p>
 * <p>Queue: {@code app.audit.event}</p>
 * <p>DLQ: {@code app.audit.event.dlq}</p>
 */
@Slf4j
@Component
public class AuditListener {

    // TODO: Inject your audit service here
    // private final AuditService auditService;

    @RabbitListener(queues = QueueConstants.AUDIT_QUEUE)
    public void onMessage(String payload) {
        log.info("Audit event received: {}", payload);

        // TODO: Deserialize and persist
        // AuditMessage event = objectMapper.readValue(payload, AuditMessage.class);
        // auditService.record(event);
    }
}
