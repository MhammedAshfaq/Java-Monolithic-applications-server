package com.javainfraexample.spring_monolith_template.messaging.email.constant;

/**
 * Email-specific constants — types used to route email processing logic.
 *
 * <p>Queue/routing key names are in {@code QueueConstants} (shared registry).
 * This class holds domain-specific values used by publishers, listeners, and EmailService.</p>
 *
 * <h3>Adding a new email type:</h3>
 * <ol>
 *   <li>Add a constant here (e.g. {@code TYPE_INVOICE})</li>
 *   <li>Add a convenience method in {@code EmailPublisher}</li>
 *   <li>Add a case in {@code EmailService.process()}</li>
 * </ol>
 */
public final class EmailConstants {

    private EmailConstants() {}

    // ===========================================
    // Email Types (determines which handler in EmailService)
    // ===========================================

    /** Welcome email — sent when a new user registers. */
    public static final String TYPE_WELCOME = "WELCOME";

    /** Login notification — sent when a user logs in. */
    public static final String TYPE_LOGIN = "LOGIN";

    /** Profile update confirmation — sent when user updates their profile. */
    public static final String TYPE_USER_UPDATE = "USER_UPDATE";

    /** Password reset email — sent when user requests password reset. */
    public static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";

    /** Password changed confirmation — sent after successful password change. */
    public static final String TYPE_PASSWORD_CHANGED = "PASSWORD_CHANGED";

    /** Account verification — sent for email verification. */
    public static final String TYPE_VERIFICATION = "VERIFICATION";
}
