package com.javainfraexample.spring_monolith_template.messaging.email.listener;

import com.javainfraexample.spring_monolith_template.messaging.constant.QueueConstants;
import com.javainfraexample.spring_monolith_template.messaging.email.message.EmailMessage;
import com.javainfraexample.spring_monolith_template.services.email.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the email queue and delegates to {@code EmailService}.
 *
 * <p>This listener only handles delegation — all business
 * logic (switch case per email type) lives in {@code EmailService}.</p>
 *
 * <p>Spring's {@code JacksonJsonMessageConverter} automatically deserializes
 * the JSON payload into {@code EmailMessage} — no manual ObjectMapper needed.</p>
 *
 * <p>Retry: Spring auto-retry (3 attempts, exponential backoff) → DLQ on exhaustion.</p>
 * <p>Queue: {@code app.email.send}</p>
 * <p>DLQ: {@code app.email.send.dlq}</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailListener {

    private final EmailService emailService;

    @RabbitListener(queues = QueueConstants.EMAIL_QUEUE)
    public void onMessage(EmailMessage email) {
        log.info("[EMAIL QUEUE] Message received: type={}, to={}", email.type(), email.to());

        emailService.process(email);

        log.info("[EMAIL QUEUE] Completed: type={}, to={}", email.type(), email.to());
    }
}
