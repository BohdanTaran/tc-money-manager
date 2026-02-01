package org.tc.mtracker.auth;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.auth.dto.AuthRequestDTO;
import org.tc.mtracker.auth.dto.AuthResponseDTO;
import org.tc.mtracker.auth.dto.LoginRequestDto;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtResponseDTO;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.User;
import org.tc.mtracker.user.UserRepository;
import org.tc.mtracker.user.UserService;
import org.tc.mtracker.utils.exceptions.UserAlreadyActivatedException;
import org.tc.mtracker.utils.exceptions.UserAlreadyExistsException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public AuthResponseDTO signUp(AuthRequestDTO dto) {
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

        return new AuthResponseDTO(save.getId(), save.getFullName(), save.getEmail(), save.getCurrencyCode(), save.isActivated());
    }

    public JwtResponseDTO login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.email()).orElseThrow(
                () -> new BadCredentialsException("User with email " + dto.email() + " does not exist.")
        );

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials. Password does not match!");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateToken(userDetails);

        log.info("User with id {} is authenticated successfully.", user.getId());
        return new JwtResponseDTO(accessToken);
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
