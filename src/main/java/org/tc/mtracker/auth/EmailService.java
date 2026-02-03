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

    private static final String PURPOSE_CLAIM_KEY = "purpose";
    private static final String EMAIL_VERIFICATION_PURPOSE = "email_verification";
    private static final String EMAIL_VERIFICATION_SUBJECT = "Email Verification";
    private static final String VERIFICATION_PATH_AND_QUERY = "/api/v1/auth/verify?token=%s";

    private final JwtService jwtService;
    private final JavaMailSender javaMailSender;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    public void sendVerificationEmail(User user) {
        String token = generateVerificationToken(user);
        String verificationLink = buildVerificationLink(token);

        sendPlainTextEmail(
                user.getEmail(),
                EMAIL_VERIFICATION_SUBJECT,
                "Please verify your email by clicking this link: " + verificationLink
        );
    }

    private void sendPlainTextEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        javaMailSender.send(message);
    }

    private String buildVerificationLink(String token) {
        return frontendUrl + String.format(VERIFICATION_PATH_AND_QUERY, token);
    }

    private String generateVerificationToken(User user) {
        return jwtService.generateToken(
                Map.of(PURPOSE_CLAIM_KEY, EMAIL_VERIFICATION_PURPOSE),
                new CustomUserDetails(user)
        );
    }
}
