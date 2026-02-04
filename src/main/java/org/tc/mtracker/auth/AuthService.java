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
import org.tc.mtracker.auth.dto.AuthRequestDTO;
import org.tc.mtracker.auth.dto.AuthResponseDTO;
import org.tc.mtracker.auth.dto.LoginRequestDto;
import org.tc.mtracker.image.S3Service;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.UserAlreadyActivatedException;
import org.tc.mtracker.utils.exceptions.UserAlreadyExistsException;

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

    @Transactional
    public AuthResponseDTO signUp(AuthRequestDTO dto, MultipartFile avatar) {
        if (userService.isExistsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        String imageKey = UUID.randomUUID().toString();
        String avatarUrl = uploadAvatar(imageKey, avatar);

        User user = buildUserForSignUp(dto, imageKey);
        User savedUser = userService.save(user);

        emailService.sendVerificationEmail(user);
        log.info("User with id {} is registered successfully.", savedUser.getId());
        return toAuthResponse(savedUser, avatarUrl);
    }

    public JwtResponseDTO login(LoginRequestDto dto) {
        User user = loadUserByEmailOrThrow(dto.email());
        validatePasswordOrThrow(dto.password(), user.getPassword());

        String accessToken = issueAccessToken(user);

        log.info("User with id {} is authenticated successfully.", user.getId());
        return new JwtResponseDTO(accessToken);
    }

    public JwtResponseDTO verifyToken(String token) {
        validateEmailVerificationTokenPurpose(token);

        String email = jwtService.extractUsername(token);
        User user = userService.findByEmail(email);

        if (user.isActivated()) {
            throw new UserAlreadyActivatedException("User with email " + email + " is already activated");
        }

        user.setActivated(true);
        userService.save(user);

        String accessToken = issueAccessToken(user);
        log.info("User with id {} is verified successfully.", user.getId());
        return new JwtResponseDTO(accessToken);
    }

    private User buildUserForSignUp(AuthRequestDTO dto, String imageKey) {
        return User.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .currencyCode(dto.currencyCode())
                .avatarId(imageKey)
                .isActivated(false)
                .build();
    }

    private AuthResponseDTO toAuthResponse(User savedUser, @Nullable String avatarUrl) {
        return new AuthResponseDTO(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getCurrencyCode(),
                avatarUrl,
                savedUser.isActivated()
        );
    }

    private User loadUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new BadCredentialsException("User with email " + email + " does not exist.")
        );
    }

    private void validatePasswordOrThrow(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BadCredentialsException("Invalid credentials. Password does not match!");
        }
    }

    private void validateEmailVerificationTokenPurpose(String token) {
        String purpose = jwtService.extractClaim(token, claims -> claims.get("purpose", String.class));
        if (!EMAIL_VERIFICATION_PURPOSE.equals(purpose)) {
            throw new JwtException("Invalid token type for verification");
        }
    }

    private String issueAccessToken(User user) {
        return jwtService.generateToken(new CustomUserDetails(user));
    }

    private @Nullable String uploadAvatar(String imageKey, MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }

        imageService.saveFile(imageKey, avatar);
        return imageService.generatePresignedUrl(imageKey);
    }
}


