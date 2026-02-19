package org.tc.mtracker.auth;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.auth.dto.*;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.exceptions.FileStorageException;
import org.tc.mtracker.utils.exceptions.UserAlreadyActivatedException;
import org.tc.mtracker.utils.exceptions.UserAlreadyExistsException;
import org.tc.mtracker.utils.exceptions.UserResetPasswordException;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String EMAIL_VERIFICATION_PURPOSE = "email_verification";

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final S3Service imageService;
    private final RefreshTokenService refreshTokenService;

    private final AuthMapper authMapper;

    @Transactional
    public AuthResponseDTO registerUser(AuthRequestDTO dto, MultipartFile avatar) {
        if (userService.isExistsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        String imageKey = null;
        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            imageKey = UUID.randomUUID().toString();
            avatarUrl = uploadAvatar(imageKey, avatar);
        }
        User user = User.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .currencyCode(dto.currencyCode())
                .avatarId(imageKey)
                .isActivated(false)
                .build();
        User savedUser = userService.save(user);

        sendVerificationEmail(savedUser);

        log.info("User with id {} is registered successfully.", savedUser.getId());
        return authMapper.toAuthResponseDTO(savedUser, avatarUrl);
    }

    public JwtResponseDTO login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.email()).orElseThrow(
                () -> new BadCredentialsException("User with email " + dto.email() + " does not exist.")
        );

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials. Password does not match!");
        }

        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User with id {} is authenticated successfully.", user.getId());
        return new JwtResponseDTO(accessToken, refreshToken.getToken());
    }

    public void sendTokenToResetPassword(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new BadCredentialsException("User with email " + email + " does not exist.")
        );

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String resetToken = jwtService.generateToken(Map.of("purpose", "password_reset"), userDetails);

        emailService.sendResetPassword(user, resetToken);
        log.info("Reset password token sent to user's email with id: {}", user.getId());
    }

    @Transactional
    public JwtResponseDTO resetPassword(String token, ResetPasswordDTO dto) {
        verifyTokenPurpose(token, "password_reset");
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new UserResetPasswordException("Passwords do not match!");
        }

        String email = jwtService.extractUsername(token);
        User user = userService.findByEmail(email);

        user.setPassword(passwordEncoder.encode(dto.password()));
        userService.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("Password successfully changed for user with id: {}", user.getId());
        return new JwtResponseDTO(accessToken, refreshToken.getToken());
    }

    public JwtResponseDTO verifyToken(String token) {
        verifyTokenPurpose(token, EMAIL_VERIFICATION_PURPOSE);

        String email = jwtService.extractUsername(token);
        User user = userService.findByEmail(email);

        if (user.isActivated()) {
            throw new UserAlreadyActivatedException("User with email " + email + " is already activated");
        }

        user.setActivated(true);
        userService.save(user);

        String accessToken = jwtService.generateToken(new CustomUserDetails(user));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User with id {} is verified successfully.", user.getId());
        return new JwtResponseDTO(accessToken, refreshToken.getToken());
    }

    public JwtResponseDTO refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(new CustomUserDetails(user));
                    return new JwtResponseDTO(accessToken, request.refreshToken());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database"));
    }

    private void sendVerificationEmail(User user) {
        String token = generateEmailVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    private String generateEmailVerificationToken(User user) {
        return jwtService.generateToken(
                Map.of("purpose", EMAIL_VERIFICATION_PURPOSE),
                new CustomUserDetails(user)
        );
    }

    private void verifyTokenPurpose(String token, String requiredPurpose) {
        String purpose = jwtService.extractClaim(token, claims -> claims.get("purpose", String.class));
        if (!requiredPurpose.equals(purpose)) {
            throw new JwtException("Invalid_token_purpose");
        }
    }

    private @Nullable String uploadAvatar(String imageKey, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }

        try {
            imageService.saveFile(imageKey, avatar);
        } catch (FileStorageException ex){
            log.error("Error while uploading avatar: {}", ex.getMessage());
            return null;
        }
        return imageService.generatePresignedUrl(imageKey);
    }
}


