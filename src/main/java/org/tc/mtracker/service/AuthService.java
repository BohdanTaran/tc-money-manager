package org.tc.mtracker.service;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.dto.JwtResponseDTO;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.dto.UserSignedUpResponseDTO;
import org.tc.mtracker.entity.UserEntity;
import org.tc.mtracker.exceptions.UserIsAlreadyActivatedException;
import org.tc.mtracker.exceptions.UserWithThisEmailAlreadyExistsException;
import org.tc.mtracker.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public UserSignedUpResponseDTO signUp(UserSignUpRequestDTO dto) {
        if (userService.isExistsByEmail(dto.email())) {
            throw new UserWithThisEmailAlreadyExistsException("User with this email already exists");
        }
        UserEntity userEntity = UserEntity.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .currencyCode(dto.currencyCode())
                .isActivated(false)
                .build();

        UserEntity save = userService.save(userEntity);
        emailService.sendVerificationEmail(userEntity);

        return new UserSignedUpResponseDTO(save.getId(), save.getFullName(), save.getEmail(), save.getCurrencyCode(), save.isActivated());
    }

    public JwtResponseDTO verifyToken(String token) {
        String purpose = jwtService.extractClaim(token, claims -> claims.get("purpose", String.class));
        if (!"email_verification".equals(purpose)) {
            throw new JwtException("Invalid token type for verification");
        }

        String email = jwtService.extractUsername(token);
        UserEntity user = userService.findByEmail(email);
        if (user.isActivated()) {
            throw new UserIsAlreadyActivatedException("User with email " + email + " is already activated");
        }
        user.setActivated(true);
        userService.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);

        return new JwtResponseDTO(accessToken);
    }
}
