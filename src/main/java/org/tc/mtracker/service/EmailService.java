package org.tc.mtracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.tc.mtracker.entity.UserEntity;
import org.tc.mtracker.security.CustomUserDetails;

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

    public void sendVerificationEmail(UserEntity user) {
        String token = generateVerificationToken(user);
        String verificationLink = String.format("%s/api/v1/auth/verify?token=%s", frontendUrl, token);

        sendEmail(user.getEmail(),
                "Email Verification",
                "Please verify your email by clicking this link: " + verificationLink);
    }

    public String generateVerificationToken(UserEntity userEntity) {
        return jwtService.generateToken(
                Map.of("purpose", "email_verification"),
                new CustomUserDetails(userEntity)
        );
    }
}
