package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.auth.dto.ResetPasswordDTO;
import org.tc.mtracker.utils.exceptions.UserResetPasswordException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public void changeUserPassword(User user, ResetPasswordDTO dto) {
        if (!dto.password().equals(dto.confirmPassword())) {
            throw new UserResetPasswordException("Passwords do not match!");
        }

        user.setPassword(passwordEncoder.encode(dto.password()));
        userRepository.save(user);

        log.info("Password successfully changed for user with id: {}", user.getId());
    }
}
