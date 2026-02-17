package com.javainfraexample.spring_monolith_template.messaging.email.message;

import java.util.Map;

/**
 * Unified email message payload — the {@code type} field determines
 * which handler processes it in {@code EmailService}.
 *
 * <h3>Types:</h3>
 * <ul>
 *   <li><b>WELCOME</b> — new user registration</li>
 *   <li><b>LOGIN</b> — user login notification</li>
 *   <li><b>USER_UPDATE</b> — profile update confirmation</li>
 *   <li><b>PASSWORD_RESET</b> — password reset link</li>
 *   <li><b>PASSWORD_CHANGED</b> — password change confirmation</li>
 *   <li><b>VERIFICATION</b> — email verification</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * <pre>
 * EmailMessage.welcome("user@example.com", "John", Map.of());
 * EmailMessage.login("user@example.com", "John", Map.of("ip", "192.168.1.1"));
 * EmailMessage.userUpdate("user@example.com", "John", Map.of("field", "email"));
 * </pre>
 *
 * @param type     email type: WELCOME, LOGIN, USER_UPDATE, PASSWORD_RESET, etc.
 * @param to       recipient email address
 * @param name     recipient display name
 * @param subject  email subject line
 * @param body     email body (plain text or HTML)
 * @param data     additional payload (e.g. reset link, IP address, changed fields)
 */
public record EmailMessage(
        String type,
        String to,
        String name,
        String subject,
        String body,
        Map<String, Object> data
) {

    /** Welcome email — new user registration. */
    public static EmailMessage welcome(String to, String name, Map<String, Object> data) {
        return new EmailMessage("WELCOME", to, name,
                "Welcome to Our Platform!",
                "Hi " + name + ", thanks for signing up! We're excited to have you on board.",
                data);
    }

    /** Login notification — user logged in. */
    public static EmailMessage login(String to, String name, Map<String, Object> data) {
        return new EmailMessage("LOGIN", to, name,
                "Login Notification",
                "Hi " + name + ", you have successfully logged in.",
                data);
    }

    /** Profile update confirmation — user updated their profile. */
    public static EmailMessage userUpdate(String to, String name, Map<String, Object> data) {
        return new EmailMessage("USER_UPDATE", to, name,
                "Profile Updated",
                "Hi " + name + ", your profile has been updated successfully.",
                data);
    }

    /** Password reset — user requested password reset. */
    public static EmailMessage passwordReset(String to, String name, Map<String, Object> data) {
        return new EmailMessage("PASSWORD_RESET", to, name,
                "Password Reset Request",
                "Hi " + name + ", click the link below to reset your password.",
                data);
    }

    /** Password changed — password was successfully changed. */
    public static EmailMessage passwordChanged(String to, String name, Map<String, Object> data) {
        return new EmailMessage("PASSWORD_CHANGED", to, name,
                "Password Changed",
                "Hi " + name + ", your password has been changed successfully.",
                data);
    }

    /** Email verification — verify email address. */
    public static EmailMessage verification(String to, String name, Map<String, Object> data) {
        return new EmailMessage("VERIFICATION", to, name,
                "Verify Your Email",
                "Hi " + name + ", please verify your email address by clicking the link below.",
                data);
    }
}
