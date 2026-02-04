package org.tc.mtracker.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.User;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JwtService jwtService;
    private final JavaMailSender javaMailSender;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    public void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
    }

    public void sendVerificationEmail(User user) {
        String token = generateVerificationToken(user);
        String verificationLink = String.format("%s/api/v1/auth/verify?token=%s", frontendUrl, token);

        sendEmail(user.getEmail(),
                "Email Verification",
                "Please verify your email by clicking this link: " + verificationLink);
    }
    /**
     * Generates an email with link
     * @param user requested user
     * @param resetToken generated token
     */
    public void sendResetPassword(User user, String resetToken) {
        String verificationLink = String.format("%s/api/v1/auth/verify?resetToken=%s", frontendUrl, resetToken);

        sendEmail(user.getEmail(),
                "Reset Password",
                "Please click on this link within 24 hours to reset your password: " + verificationLink);
    }

    public String generateVerificationToken(User user) {
        return jwtService.generateToken(
                Map.of("purpose", "email_verification"),
                new CustomUserDetails(user)
        );
    }
}
