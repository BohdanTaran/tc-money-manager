package org.tc.mtracker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.auth.dto.*;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.*;
import org.tc.mtracker.utils.exceptions.TokenNotFoundException;
import org.tc.mtracker.utils.exceptions.UserAlreadyExistsException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AuthResponseDTO register(AuthRequestDTO dto, MultipartFile avatar) { //todo two endpoints
        if (userService.existsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("User with this newEmail already exists");
        }

        User user = buildUserFromAuthRequest(dto);
        userService.uploadAvatar(avatar, user);
        User savedUser = userService.save(user);
        sendVerificationEmail(user);

        log.info("User with id {} is registered successfully.", savedUser.getId());
        return authMapper.toAuthResponseDTO(savedUser, userService.generateAvatarUrl(savedUser));
    }


    public JwtResponseDTO login(LoginRequestDTO dto) {
        User user = getUser(dto.email());
        verifyPassword(dto.password(), user.getPassword());

        log.info("User with id {} is authenticated successfully.", user.getId());
        return createJwtResponseDTO(user);
    }

    public JwtResponseDTO refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> new JwtResponseDTO(jwtService.generateToken(new CustomUserDetails(user)), request.refreshToken()))
                .orElseThrow(() -> new TokenNotFoundException("Refresh token is not in database"));
    }

    private @NonNull User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials. User not found!"));
    }

    private void validateEmailUniqueness(AuthRequestDTO dto) {
        if (userService.existsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("User with this newEmail already exists");
        }
    }

    private void verifyPassword(String providedPassword, String expectedPassword) {
        if (!passwordEncoder.matches(providedPassword, expectedPassword)) {
            throw new BadCredentialsException("Invalid credentials. Password does not match!");
        }
    }

    private JwtResponseDTO createJwtResponseDTO(User user) {
        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new JwtResponseDTO(accessToken, refreshToken.getToken());
    }

    private void sendVerificationEmail(User user) {
        String token = generateEmailVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    private String generateEmailVerificationToken(User user) {
        return jwtService.generateToken(
                Map.of("purpose", JwtPurpose.EMAIL_VERIFICATION.getValue()),
                new CustomUserDetails(user)
        );
    }

    private User buildUserFromAuthRequest(AuthRequestDTO dto) {
        return User.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .currencyCode(dto.currencyCode())
                .isActivated(false)
                .build();
    }

    public void sendPasswordResetToken(String email) {
        passwordResetService.sendPasswordResetToken(email);
    }

    public JwtResponseDTO resetPassword(String token, ResetPasswordDTO resetPasswordDTO) {
        return passwordResetService.resetPassword(token, resetPasswordDTO);
    }

    public JwtResponseDTO processEmailVerification(String token) {
        return emailVerificationService.processEmailVerification(token);
    }
}


