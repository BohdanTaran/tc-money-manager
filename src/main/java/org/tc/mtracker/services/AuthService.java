package org.tc.mtracker.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tc.mtracker.dto.UserSignUpRequestDTO;
import org.tc.mtracker.entity.UserEntity;
import org.tc.mtracker.exceptions.UserWithThisEmailAlreadyExistsException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserEntity signUp(UserSignUpRequestDTO dto) {
        //якщо юзер з цим email вже є кидаємо помилку
        if (userService.isExistsByEmail(dto.email())) {
            throw new UserWithThisEmailAlreadyExistsException("User with this email already exists");
        }
        //якщо немає то валідуємо і реєструємо
        UserEntity userEntity = UserEntity.builder()
                .email(dto.email())
                .fullName(dto.fullName())
                .password(passwordEncoder.encode(dto.password()))
                .isActivated(true) //TODO EMAIL VERIFICATION
                .build();

        UserEntity save = userService.save(userEntity);
        return save;
    }
}
