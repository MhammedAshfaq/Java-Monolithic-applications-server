package com.javainfraexample.spring_monolith_template.messaging.dlq;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Sends Slack webhook alerts when a message lands in the DLQ.
 *
 * <p>Configure the webhook URL in {@code application.yaml}:</p>
 * <pre>
 * app:
 *   dlq:
 *     slack:
 *       enabled: true
 *       webhook-url: https://hooks.slack.com/services/XXX/YYY/ZZZ
 * </pre>
 */
@Slf4j
@Component
public class DlqSlackNotifier {

    @Value("${app.dlq.slack.enabled:false}")
    private boolean enabled;

    @Value("${app.dlq.slack.webhook-url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send a Slack alert for a failed DLQ message.
     */
    public void notify(DlqMessage dlqMessage) {
        if (!enabled || webhookUrl.isBlank()) {
            log.debug("Slack DLQ notification disabled or webhook URL not configured");
            return;
        }

        String slackPayload = buildSlackPayload(dlqMessage);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(slackPayload, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("Slack DLQ alert sent: queue={}, type={}", dlqMessage.originalQueue(), dlqMessage.messageType());

        } catch (Exception e) {
            log.error("Failed to send Slack DLQ alert: {}", e.getMessage(), e);
        }
    }

    private String buildSlackPayload(DlqMessage msg) {
        return """
                {
                  "blocks": [
                    {
                      "type": "header",
                      "text": {
                        "type": "plain_text",
                        "text": "🚨 DLQ Alert — Message Failed",
                        "emoji": true
                      }
                    },
                    {
                      "type": "section",
                      "fields": [
                        { "type": "mrkdwn", "text": "*Queue:*\\n`%s`" },
                        { "type": "mrkdwn", "text": "*Type:*\\n`%s`" },
                        { "type": "mrkdwn", "text": "*Retries:*\\n`%d`" },
                        { "type": "mrkdwn", "text": "*Failed At:*\\n`%s`" }
                      ]
                    },
                    {
                      "type": "section",
                      "text": {
                        "type": "mrkdwn",
                        "text": "*Error:*\\n```%s```"
                      }
                    },
                    {
                      "type": "section",
                      "text": {
                        "type": "mrkdwn",
                        "text": "*Payload:*\\n```%s```"
                      }
                    }
                  ]
                }
                """.formatted(
                msg.originalQueue(),
                msg.messageType(),
                msg.retryCount(),
                msg.failedAt().toString(),
                truncate(msg.errorReason(), 500),
                truncate(msg.payload(), 1000)
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
