package com.javainfraexample.spring_monolith_template.messaging.notification;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notification queue infrastructure — declares all 3 notification queues, DLQs, and bindings.
 *
 * <h3>Notification types:</h3>
 * <ul>
 *   <li><b>Single</b> — one-to-one notification to a specific user</li>
 *   <li><b>Multicast</b> — one-to-many notification to a list of users</li>
 *   <li><b>Topic</b> — broadcast to all users subscribed to a topic</li>
 * </ul>
 *
 * <h3>Queue layout:</h3>
 * <pre>
 *   app.notification.single      → SingleNotificationListener      (DLQ: app.notification.single.dlq)
 *   app.notification.multicast   → MulticastNotificationListener   (DLQ: app.notification.multicast.dlq)
 *   app.notification.topic       → TopicNotificationListener       (DLQ: app.notification.topic.dlq)
 * </pre>
 */
@Configuration
public class NotificationQueueConfig {

    // ===========================================
    // Single Notification — Queue + DLQ
    // ===========================================

    @Bean
    public Queue notificationSingleQueue() {
        return QueueBuilder.durable(QueueConstants.NOTIFICATION_SINGLE_QUEUE)
                .withArgument("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QueueConstants.NOTIFICATION_SINGLE_DLQ)
                .build();
    }

    @Bean
    public Binding notificationSingleBinding(Queue notificationSingleQueue, TopicExchange mainExchange) {
        return BindingBuilder.bind(notificationSingleQueue).to(mainExchange)
                .with(QueueConstants.NOTIFICATION_SINGLE_ROUTING_KEY);
    }

    @Bean
    public Queue notificationSingleDlq() {
        return QueueBuilder.durable(QueueConstants.NOTIFICATION_SINGLE_DLQ).build();
    }

    @Bean
    public Binding notificationSingleDlqBinding(Queue notificationSingleDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(notificationSingleDlq).to(dlxExchange)
                .with(QueueConstants.NOTIFICATION_SINGLE_DLQ);
    }

    // ===========================================
    // Multicast Notification — Queue + DLQ
    // ===========================================

    @Bean
    public Queue notificationMulticastQueue() {
        return QueueBuilder.durable(QueueConstants.NOTIFICATION_MULTICAST_QUEUE)
                .withArgument("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QueueConstants.NOTIFICATION_MULTICAST_DLQ)
                .build();
    }

    @Bean
    public Binding notificationMulticastBinding(Queue notificationMulticastQueue, TopicExchange mainExchange) {
        return BindingBuilder.bind(notificationMulticastQueue).to(mainExchange)
                .with(QueueConstants.NOTIFICATION_MULTICAST_ROUTING_KEY);
    }

    @Bean
    public Queue notificationMulticastDlq() {
        return QueueBuilder.durable(QueueConstants.NOTIFICATION_MULTICAST_DLQ).build();
    }

    @Bean
    public Binding notificationMulticastDlqBinding(Queue notificationMulticastDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(notificationMulticastDlq).to(dlxExchange)
                .with(QueueConstants.NOTIFICATION_MULTICAST_DLQ);
    }

    // ===========================================
    // Topic Notification — Queue + DLQ
    // ===========================================

    @Bean
    public Queue notificationTopicQueue() {
        return QueueBuilder.durable(QueueConstants.NOTIFICATION_TOPIC_QUEUE)
                .withArgument("x-dead-letter-exchange", QueueConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", QueueConstants.NOTIFICATION_TOPIC_DLQ)
                .build();
    }

    @Bean
    public Binding notificationTopicBinding(Queue notificationTopicQueue, TopicExchange mainExchange) {
        return BindingBuilder.bind(notificationTopicQueue).to(mainExchange)
                .with(QueueConstants.NOTIFICATION_TOPIC_ROUTING_KEY);
    }

    @Bean
    public Queue notificationTopicDlq() {
        return QueueBuilder.durable(QueueConstants.NOTIFICATION_TOPIC_DLQ).build();
    }

    @Bean
    public Binding notificationTopicDlqBinding(Queue notificationTopicDlq, TopicExchange dlxExchange) {
        return BindingBuilder.bind(notificationTopicDlq).to(dlxExchange)
                .with(QueueConstants.NOTIFICATION_TOPIC_DLQ);
    }
}
