package com.javainfraexample.spring_monolith_template.config.rabbitmq;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared RabbitMQ infrastructure — exchanges, JSON converter, and template.
 *
 * <p>Queue-specific configuration (queue, DLQ, bindings) lives inside each
 * domain package under {@code messaging/}. This class only provides the
 * shared components that all domains depend on.</p>
 *
 * <h3>Message flow:</h3>
 * <pre>
 *   Publisher → app.exchange (topic) → routing key → Queue → Listener
 *                                                      ↓ (on failure)
 *                                              app.exchange.dlx → *.dlq
 * </pre>
 *
 * @see com.javainfraexample.spring_monolith_template.messaging.email.EmailQueueConfig
 * @see com.javainfraexample.spring_monolith_template.messaging.notification.NotificationQueueConfig
 * @see com.javainfraexample.spring_monolith_template.messaging.audit.AuditQueueConfig
 * @see com.javainfraexample.spring_monolith_template.messaging.task.TaskQueueConfig
 */
@Configuration
public class RabbitMQConfig {

    // ===========================================
    // JSON Message Converter
    // ===========================================

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        template.setMandatory(true);
        return template;
    }

    // ===========================================
    // Main Exchange (shared by all domains)
    // ===========================================

    @Bean
    public TopicExchange mainExchange() {
        return new TopicExchange(QueueConstants.EXCHANGE, true, false);
    }

    // ===========================================
    // Dead Letter Exchange (shared by all domains)
    // ===========================================

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(QueueConstants.DLX_EXCHANGE, true, false);
    }
}
