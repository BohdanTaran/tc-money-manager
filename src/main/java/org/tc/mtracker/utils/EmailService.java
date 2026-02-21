package org.tc.mtracker.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.tc.mtracker.user.User;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendVerificationEmail(String email, String token) {
        String verificationLink = buildLink("/verify", "token", token);
        sendPlainTextEmail(
                email,
                "Email Verification",
                "Please verify your email by clicking this link: " + verificationLink
        );
        log.info("Verification email sent to user with email {}", email);
    }

    public void sendPasswordResetEmail(User user, String resetToken) {
        String verificationLink = buildLink("/reset-password", "resetToken", resetToken);

        sendPlainTextEmail(user.getEmail(),
                "Reset Password",
                "Please click on this link within 24 hours to reset your password: " + verificationLink);
        log.info("Reset password email sent to user with email {}", user.getEmail());
    }

    private String buildLink(String path, String paramName, String token) {
        return String.format("%s%s?%s=%s", frontendUrl, path, paramName, token);
    }

    private void sendPlainTextEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
    }
}
