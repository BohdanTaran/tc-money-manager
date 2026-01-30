package org.tc.mtracker.service;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.dto.JwtResponseDTO;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.dto.UserSignUpResponseDTO;
import org.tc.mtracker.entity.User;
import org.tc.mtracker.exceptions.UserAlreadyActivatedException;
import org.tc.mtracker.exceptions.UserAlreadyExistsException;
import org.tc.mtracker.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public UserSignUpResponseDTO signUp(UserSignUpRequestDTO dto) {
        if (userService.isExistsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
        User user = User.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .currencyCode(dto.currencyCode())
                .isActivated(false)
                .build();

        User save = userService.save(user);
        emailService.sendVerificationEmail(user);

        return new UserSignUpResponseDTO(save.getId(), save.getFullName(), save.getEmail(), save.getCurrencyCode(), save.isActivated());
    }

    public JwtResponseDTO verifyToken(String token) {
        String purpose = jwtService.extractClaim(token, claims -> claims.get("purpose", String.class));
        if (!"email_verification".equals(purpose)) {
            throw new JwtException("Invalid token type for verification");
        }

        String email = jwtService.extractUsername(token);
        User user = userService.findByEmail(email);
        if (user.isActivated()) {
            throw new UserAlreadyActivatedException("User with email " + email + " is already activated");
        }
        user.setActivated(true);
        userService.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);

        return new JwtResponseDTO(accessToken);
    }
}
