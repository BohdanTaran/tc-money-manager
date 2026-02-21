package org.tc.mtracker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.auth.dto.ResetPasswordDTO;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtPurpose;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserPasswordService;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.EmailService;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final JwtService jwtService;
    private final UserService userService;
    private final UserPasswordService userPasswordService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    public void sendPasswordResetToken(String email) {
        if (!userService.existsByEmail(email)) return;
        User user = userService.findByEmail(email);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String resetToken = jwtService.generateToken(Map.of("purpose", JwtPurpose.PASSWORD_RESET.getValue()), userDetails);

        emailService.sendPasswordResetEmail(user, resetToken);
        log.info("Reset password token sent to user's email with id: {}", user.getId());
    }

    @Transactional
    public JwtResponseDTO resetPassword(String token, ResetPasswordDTO dto) {
        verifyResetToken(token);
        User user = getUser(token);

        userPasswordService.changeUserPassword(user, dto);

        return createJwtResponseDTO(user);
    }


    private JwtResponseDTO createJwtResponseDTO(User user) {
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new JwtResponseDTO(accessToken, refreshToken.getToken());
    }

    private User getUser(String token) {
        String email = jwtService.extractUsername(token);
        return userService.findByEmail(email);
    }

    private void verifyResetToken(String token) {
        jwtService.validateToken(token, JwtPurpose.PASSWORD_RESET.getValue());
    }
}
