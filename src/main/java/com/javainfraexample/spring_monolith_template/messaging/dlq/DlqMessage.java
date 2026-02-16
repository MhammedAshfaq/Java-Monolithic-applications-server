package com.javainfraexample.spring_monolith_template.messaging.dlq;

import java.time.Instant;
import java.util.Map;

/**
 * Unified DLQ message — enriched payload built when a dead-lettered message arrives.
 *
 * <h3>Example:</h3>
 * <pre>
 * {
 *   "originalQueue": "app.notification.single",
 *   "messageType": "SINGLE_NOTIFICATION",
 *   "retryCount": 3,
 *   "errorReason": "FCM timeout",
 *   "payload": { ... },
 *   "failedAt": "2026-02-16T09:10:00Z"
 * }
 * </pre>
 *
 * @param originalQueue the source queue where the message originally failed
 * @param messageType   human-readable type (e.g. EMAIL, SINGLE_NOTIFICATION, AUDIT)
 * @param retryCount    number of attempts before landing in DLQ
 * @param errorReason   the reason for failure (from x-death header or "unknown")
 * @param payload       the original message body as a string
 * @param headers       original message headers (for debugging)
 * @param failedAt      timestamp when the message entered the DLQ
 */
public record DlqMessage(
        String originalQueue,
        String messageType,
        int retryCount,
        String errorReason,
        String payload,
        Map<String, Object> headers,
        Instant failedAt
) {
}
