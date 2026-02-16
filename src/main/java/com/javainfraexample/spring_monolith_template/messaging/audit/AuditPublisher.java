package com.javainfraexample.spring_monolith_template.messaging.audit;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Publishes audit events to the audit queue.
 *
 * <h3>Usage:</h3>
 * <pre>
 * auditPublisher.send(new AuditMessage("USER_REGISTERED", userId, Map.of("email", email)));
 * auditPublisher.send("LOGIN_FAILED", null, request.getRemoteAddr(), Map.of("email", email));
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send an audit event to the queue.
     */
    public void send(AuditMessage message) {
        rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, QueueConstants.AUDIT_ROUTING_KEY, message);
        log.info("Audit event published: action={}, userId={}", message.action(), message.userId());
    }

    /**
     * Convenience: build and send an audit event with auto-timestamp.
     */
    public void send(String action, String userId, Map<String, Object> details) {
        send(new AuditMessage(action, userId, details));
    }

    /**
     * Convenience: build and send an audit event with IP and auto-timestamp.
     */
    public void send(String action, String userId, String ip, Map<String, Object> details) {
        send(new AuditMessage(action, userId, ip, details, Instant.now()));
    }
}
