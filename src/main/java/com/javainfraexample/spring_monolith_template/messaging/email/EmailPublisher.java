package com.javainfraexample.spring_monolith_template.messaging.email;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes email messages to the email queue.
 *
 * <h3>Usage:</h3>
 * <pre>
 * emailPublisher.send(new EmailMessage("user@example.com", "Welcome!", "Thanks for signing up."));
 * emailPublisher.send("user@example.com", "Password Reset", "Click here to reset...", "password-reset");
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send an email message to the queue.
     */
    public void send(EmailMessage message) {
        rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, QueueConstants.EMAIL_ROUTING_KEY, message);
        log.info("Email published: to={}, subject={}", message.to(), message.subject());
    }

    /**
     * Convenience: build and send an email message.
     */
    public void send(String to, String subject, String body, String template) {
        send(new EmailMessage(to, subject, body, template));
    }

    /**
     * Convenience: send without template.
     */
    public void send(String to, String subject, String body) {
        send(new EmailMessage(to, subject, body));
    }
}
