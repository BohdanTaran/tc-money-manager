package org.tc.mtracker.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.auth.dto.*;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtPurpose;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.EmailService;
import org.tc.mtracker.utils.exceptions.TokenNotFoundException;
import org.tc.mtracker.utils.exceptions.UserAlreadyActivatedException;
import org.tc.mtracker.utils.exceptions.UserAlreadyExistsException;
import org.tc.mtracker.utils.exceptions.UserResetPasswordException;

import java.util.Map;
import java.util.Optional;

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

    @Transactional
    public AuthResponseDTO register(AuthRequestDTO dto, MultipartFile avatar) {
        if (userService.existsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("User with this newEmail already exists");
        }

        User user = User.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .currencyCode(dto.currencyCode())
                .isActivated(false)
                .build();

        User savedUser = userService.save(user);

        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            userService.uploadAvatar(avatar, savedUser);
            userService.save(savedUser);
            avatarUrl = userService.generateAvatarUrl(savedUser);
        }

        sendVerificationEmail(savedUser);

        log.info("User with id {} is registered successfully.", savedUser.getId());
        return authMapper.toAuthResponseDTO(savedUser, avatarUrl);
    }

    public JwtResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials. User not found!"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials. Password does not match!");
        }

        log.info("User with id {} is authenticated successfully.", user.getId());
        return createJwtResponseDTO(user);
    }

    public void sendPasswordResetToken(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return;
        }
        User user = userOptional.get();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String resetToken = jwtService.generateToken(Map.of("purpose", JwtPurpose.PASSWORD_RESET.getValue()), userDetails);

        emailService.sendPasswordResetEmail(user, resetToken);
        log.info("Reset password token sent to user's email with id: {}", user.getId());
    }

    @Transactional
    public JwtResponseDTO resetPassword(String token, ResetPasswordDTO dto) {
        jwtService.validateToken(token, JwtPurpose.PASSWORD_RESET.getValue());
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new UserResetPasswordException("Passwords do not match!");
        }

        String email = jwtService.extractUsername(token);
        User user = userService.findByEmail(email);

        user.setPassword(passwordEncoder.encode(dto.password()));
        userService.save(user);

        log.info("Password successfully changed for user with id: {}", user.getId());
        return createJwtResponseDTO(user);
    }

    @Transactional
    public JwtResponseDTO verifyToken(String token) {
        jwtService.validateToken(token, JwtPurpose.EMAIL_VERIFICATION.getValue());

        String email = jwtService.extractUsername(token);
        User user = userService.findByEmail(email);

        if (user.isActivated()) {
            throw new UserAlreadyActivatedException("User with email " + email + " is already activated");
        }

        user.setActivated(true);
        userService.save(user);

        log.info("User with id {} is verified successfully.", user.getId());
        return createJwtResponseDTO(user);
    }

    public JwtResponseDTO refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> new JwtResponseDTO(jwtService.generateToken(new CustomUserDetails(user)), request.refreshToken()))
                .orElseThrow(() -> new TokenNotFoundException("Refresh token is not in database"));
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
}


