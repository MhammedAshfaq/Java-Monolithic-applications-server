package com.javainfraexample.spring_monolith_template.messaging.email;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;

import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the email queue and delegates to the email service.
 *
 * <p>Retry: Spring auto-retry (3 attempts, exponential backoff) → DLQ on exhaustion.</p>
 * <p>Queue: {@code app.email.send}</p>
 * <p>DLQ: {@code app.email.send.dlq}</p>
 */
@Slf4j
@Component
public class EmailListener {

    // TODO: Inject your email service here
    // private final EmailService emailService;

    @RabbitListener(queues = QueueConstants.EMAIL_QUEUE)
    public void onMessage(String payload) {
        log.info("Email message received: {}", payload);

        // TODO: Deserialize and call business service
        // EmailMessage email = objectMapper.readValue(payload, EmailMessage.class);
        // emailService.send(email.to(), email.subject(), email.body());
    }
}
