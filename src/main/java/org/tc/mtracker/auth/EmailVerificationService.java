package org.tc.mtracker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtPurpose;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserEmailService;
import org.tc.mtracker.user.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final JwtService jwtService;
    private final UserService userService;
    private final UserEmailService userEmailService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public JwtResponseDTO processEmailVerification(String token) {
        verifyEmailToken(token);

        User user = getUser(token);

        userEmailService.activateUser(user);

        return createJwtResponseDTO(user);
    }

    private User getUser(String token) {
        String email = jwtService.extractUsername(token);
        return userService.findByEmail(email);
    }

    private void verifyEmailToken(String token) {
        jwtService.validateToken(token, JwtPurpose.EMAIL_VERIFICATION.getValue());
    }

    private JwtResponseDTO createJwtResponseDTO(User user) {
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new JwtResponseDTO(accessToken, refreshToken.getToken());
    }
}
