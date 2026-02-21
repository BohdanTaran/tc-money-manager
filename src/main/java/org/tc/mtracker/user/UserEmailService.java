package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtPurpose;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.utils.EmailService;
import org.tc.mtracker.utils.exceptions.EmailVerificationException;
import org.tc.mtracker.utils.exceptions.UserAlreadyActivatedException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEmailService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final EmailService emailService;

    public void sendVerificationEmail(User user) {
        String token = generateEmailVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    private String generateEmailVerificationToken(User user) {
        return jwtService.generateToken(
                Map.of("purpose", JwtPurpose.EMAIL_VERIFICATION.getValue()),
                new CustomUserDetails(user)
        );
    }

    @Transactional
    public void activateUser(User user) {
        if (user.isActivated()) {
            throw new UserAlreadyActivatedException("User with email " + user.getEmail() + " is already activated");
        }
        user.setActivated(true);
        userRepository.save(user);
        log.info("User with id {} is verified successfully.", user.getId());
    }

    @Transactional
    public void sendEmailUpdateVerification(User user, String newEmail) {
        String generatedToken = jwtService.generateToken(
                Map.of("purpose", JwtPurpose.EMAIL_UPDATE_VERIFICATION.getValue()),
                new CustomUserDetails(user)
        );

        user.setPendingEmail(newEmail);
        user.setVerificationToken(generatedToken);
        userRepository.save(user);

        emailService.sendVerificationEmail(newEmail, generatedToken);
    }

    @Transactional
    public void verifyEmailUpdate(User user, String token) {
        jwtService.validateToken(token, JwtPurpose.EMAIL_UPDATE_VERIFICATION.getValue());

        if (user.getVerificationToken() == null || !token.equals(user.getVerificationToken())) {
            throw new EmailVerificationException("Invalid token for verification");
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setVerificationToken(null);
        userRepository.save(user);
        log.info("Email updated successfully for user with id: {}", user.getId());
    }
}
