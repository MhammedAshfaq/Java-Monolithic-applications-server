package com.javainfraexample.spring_monolith_template.messaging.audit;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Audit queue infrastructure — queue, DLQ, and bindings.
 *
 * <h3>Flow:</h3>
 * <pre>
 *   AuditPublisher
 *       → app.exchange (routing key: app.audit.event)
 *           → app.audit.event (queue)
 *               → AuditListener
 *                   ✓ ACK  → message removed
 *                   ✗ NACK → app.exchange.dlx
 *                               → app.audit.event.dlq
 * </pre>
 */
@Configuration
public class AuditQueueConfig {

    // ===========================================
    // Main Queue
    // ===========================================

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(QueueConstants.AUDIT_QUEUE)
                .withArgument("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QueueConstants.AUDIT_DLQ)
                .build();
    }

    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange mainExchange) {
        return BindingBuilder.bind(auditQueue).to(mainExchange).with(QueueConstants.AUDIT_ROUTING_KEY);
    }

    // ===========================================
    // Dead Letter Queue
    // ===========================================

    @Bean
    public Queue auditDlq() {
        return QueueBuilder.durable(QueueConstants.AUDIT_DLQ).build();
    }

    @Bean
    public Binding auditDlqBinding(Queue auditDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(auditDlq).to(dlxExchange).with(QueueConstants.AUDIT_DLQ);
    }
}
