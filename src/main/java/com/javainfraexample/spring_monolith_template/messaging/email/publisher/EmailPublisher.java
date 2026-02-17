package com.javainfraexample.spring_monolith_template.messaging.email.publisher;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;
import com.javainfraexample.spring_monolith_template.messaging.email.message.EmailMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Publishes email messages to the email queue — routes all email types through one queue.
 *
 * <p>The {@code type} field inside {@code EmailMessage} determines how
 * {@code EmailService} processes it (switch case).</p>
 *
 * <h3>Usage:</h3>
 * <pre>
 * emailPublisher.sendWelcome("user@example.com", "John", Map.of());
 * emailPublisher.sendLogin("user@example.com", "John", Map.of("ip", "192.168.1.1"));
 * emailPublisher.sendUserUpdate("user@example.com", "John", Map.of("field", "email"));
 * emailPublisher.sendPasswordReset("user@example.com", "John", Map.of("resetLink", "https://..."));
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Send an email message — generic method, routes any type.
     */
    public void send(EmailMessage message) {
        rabbitTemplate.convertAndSend(QueueConstants.EXCHANGE, QueueConstants.EMAIL_ROUTING_KEY, message);
        log.info("Email published: type={}, to={}, subject={}", message.type(), message.to(), message.subject());
    }

    /** Publish a welcome email (new user registration). */
    public void sendWelcome(String to, String name, Map<String, Object> data) {
        send(EmailMessage.welcome(to, name, data));
    }

    /** Publish a login notification email. */
    public void sendLogin(String to, String name, Map<String, Object> data) {
        send(EmailMessage.login(to, name, data));
    }

    /** Publish a user profile update confirmation email. */
    public void sendUserUpdate(String to, String name, Map<String, Object> data) {
        send(EmailMessage.userUpdate(to, name, data));
    }

    /** Publish a password reset email. */
    public void sendPasswordReset(String to, String name, Map<String, Object> data) {
        send(EmailMessage.passwordReset(to, name, data));
    }

    /** Publish a password changed confirmation email. */
    public void sendPasswordChanged(String to, String name, Map<String, Object> data) {
        send(EmailMessage.passwordChanged(to, name, data));
    }

    /** Publish an email verification email. */
    public void sendVerification(String to, String name, Map<String, Object> data) {
        send(EmailMessage.verification(to, name, data));
    }
}
