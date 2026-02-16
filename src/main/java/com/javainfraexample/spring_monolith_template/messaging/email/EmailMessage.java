package com.javainfraexample.spring_monolith_template.messaging.email;

/**
 * Email message payload — sent to the email queue, consumed by {@code EmailListener}.
 *
 * @param to       recipient email address
 * @param subject  email subject line
 * @param body     email body (plain text or HTML)
 * @param template optional template name (e.g. "welcome", "password-reset")
 */
public record EmailMessage(
        String to,
        String subject,
        String body,
        String template
) {
    /** Convenience constructor without template. */
    public EmailMessage(String to, String subject, String body) {
        this(to, subject, body, null);
    }
}
