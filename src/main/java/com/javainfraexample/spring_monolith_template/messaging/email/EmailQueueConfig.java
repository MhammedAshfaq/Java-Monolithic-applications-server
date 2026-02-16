package com.javainfraexample.spring_monolith_template.messaging.email;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Email queue infrastructure — queue, DLQ, and bindings.
 *
 * <h3>Flow:</h3>
 * <pre>
 *   EmailPublisher
 *       → app.exchange (routing key: app.email.send)
 *           → app.email.send (queue)
 *               → EmailListener
 *                   ✓ ACK  → message removed
 *                   ✗ NACK → app.exchange.dlx
 *                               → app.email.send.dlq (inspect in RabbitMQ UI)
 * </pre>
 */
@Configuration
public class EmailQueueConfig {

    // ===========================================
    // Main Queue
    // ===========================================

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(QueueConstants.EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QueueConstants.EMAIL_DLQ)
                .build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange mainExchange) {
        return BindingBuilder.bind(emailQueue).to(mainExchange).with(QueueConstants.EMAIL_ROUTING_KEY);
    }

    // ===========================================
    // Dead Letter Queue
    // ===========================================

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(QueueConstants.EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(emailDlq).to(dlxExchange).with(QueueConstants.EMAIL_DLQ);
    }
}
