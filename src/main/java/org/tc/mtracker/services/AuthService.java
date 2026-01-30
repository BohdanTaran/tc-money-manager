package org.tc.mtracker.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tc.mtracker.dto.JwtResponseDTO;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.entity.UserEntity;
import org.tc.mtracker.exceptions.UserWithThisEmailAlreadyExistsException;
import org.tc.mtracker.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public JwtResponseDTO signUp(UserSignUpRequestDTO dto) {
        if (userService.isExistsByEmail(dto.email())) {
            throw new UserWithThisEmailAlreadyExistsException("User with this email already exists");
        }
        UserEntity userEntity = UserEntity.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .isActivated(true) //TODO EMAIL VERIFICATION
                .build();

        UserEntity save = userService.save(userEntity);

        UserDetails userDetails = new CustomUserDetails(save);
        String generatedToken = jwtService.generateToken(userDetails);
        return new JwtResponseDTO(generatedToken);
    }
}
