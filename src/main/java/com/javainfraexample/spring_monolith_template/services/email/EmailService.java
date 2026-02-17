package com.javainfraexample.spring_monolith_template.services.email;

import com.javainfraexample.spring_monolith_template.messaging.email.constant.EmailConstants;
import com.javainfraexample.spring_monolith_template.messaging.email.message.EmailMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

/**
 * Email business service — handles all email types via switch case.
 *
 * <p>Called by {@code EmailListener} after deserializing the queue message.
 * Each email type has its own handler method with specific business logic.</p>
 *
 * <h3>Adding a new email type:</h3>
 * <ol>
 *   <li>Add a constant in {@code EmailConstants} (e.g. {@code TYPE_INVOICE})</li>
 *   <li>Add a convenience factory method in {@code EmailMessage}</li>
 *   <li>Add a convenience publish method in {@code EmailPublisher}</li>
 *   <li>Add a case in {@code process()} below</li>
 *   <li>Implement the handler method (e.g. {@code handleInvoice()})</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // TODO: Inject email sending infrastructure (e.g. JavaMailSender, SendGrid, SES)
    // private final JavaMailSender mailSender;

    /**
     * Process an email message — routes to the correct handler based on type.
     *
     * @param email the deserialized email message from the queue
     * @throws IllegalArgumentException if the email type is unknown
     */
    public void process(EmailMessage email) {
        log.info("[EMAIL SERVICE] Processing email: type={}, to={}", email.type(), email.to());

        switch (email.type()) {
            case EmailConstants.TYPE_WELCOME         -> handleWelcome(email);
            case EmailConstants.TYPE_LOGIN            -> handleLogin(email);
            case EmailConstants.TYPE_USER_UPDATE      -> handleUserUpdate(email);
            case EmailConstants.TYPE_PASSWORD_RESET   -> handlePasswordReset(email);
            case EmailConstants.TYPE_PASSWORD_CHANGED -> handlePasswordChanged(email);
            case EmailConstants.TYPE_VERIFICATION     -> handleVerification(email);
            default -> throw new IllegalArgumentException("Unknown email type: " + email.type());
        }

        log.info("[EMAIL SERVICE] Email sent successfully: type={}, to={}", email.type(), email.to());
    }

    // ===========================================
    // Handler: WELCOME — New user registration
    // ===========================================

    private void handleWelcome(EmailMessage email) {
        log.info("[EMAIL] Sending welcome email to: {}", email.to());

        // TODO: Replace with actual email sending logic
        // MimeMessage message = mailSender.createMimeMessage();
        // Use a "welcome" HTML template, inject name and data
        // mailSender.send(message);

        log.info("[EMAIL] Welcome email sent to: {} (name={})", email.to(), email.name());
    }

    // ===========================================
    // Handler: LOGIN — Login notification
    // ===========================================

    private void handleLogin(EmailMessage email) {
        log.info("[EMAIL] Sending login notification to: {}", email.to());

        // TODO: Replace with actual email sending logic
        // Include login details from data (e.g. IP address, device, timestamp)
        // String ip = (String) email.data().getOrDefault("ip", "unknown");
        // String device = (String) email.data().getOrDefault("device", "unknown");

        log.info("[EMAIL] Login notification sent to: {} (name={})", email.to(), email.name());
    }

    // ===========================================
    // Handler: USER_UPDATE — Profile update confirmation
    // ===========================================

    private void handleUserUpdate(EmailMessage email) {
        log.info("[EMAIL] Sending profile update confirmation to: {}", email.to());

        // TODO: Replace with actual email sending logic
        // Include changed fields from data (e.g. "email", "name", "phone")
        // String changedField = (String) email.data().getOrDefault("field", "profile");

        log.info("[EMAIL] Profile update email sent to: {} (name={})", email.to(), email.name());
    }

    // ===========================================
    // Handler: PASSWORD_RESET — Password reset link
    // ===========================================

    private void handlePasswordReset(EmailMessage email) {
        log.info("[EMAIL] Sending password reset email to: {}", email.to());

        // TODO: Replace with actual email sending logic
        // Include reset link from data
        // String resetLink = (String) email.data().get("resetLink");

        log.info("[EMAIL] Password reset email sent to: {} (name={})", email.to(), email.name());
    }

    // ===========================================
    // Handler: PASSWORD_CHANGED — Password changed confirmation
    // ===========================================

    private void handlePasswordChanged(EmailMessage email) {
        log.info("[EMAIL] Sending password changed confirmation to: {}", email.to());

        // TODO: Replace with actual email sending logic

        log.info("[EMAIL] Password changed email sent to: {} (name={})", email.to(), email.name());
    }

    // ===========================================
    // Handler: VERIFICATION — Email verification
    // ===========================================

    private void handleVerification(EmailMessage email) {
        log.info("[EMAIL] Sending verification email to: {}", email.to());

        // TODO: Replace with actual email sending logic
        // Include verification link from data
        // String verifyLink = (String) email.data().get("verifyLink");

        log.info("[EMAIL] Verification email sent to: {} (name={})", email.to(), email.name());
    }
}
