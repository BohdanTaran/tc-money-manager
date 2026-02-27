package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tc.mtracker.auth.dto.ResetPasswordDTO;
import org.tc.mtracker.user.dto.UpdateUserPasswordRequestDTO;
import org.tc.mtracker.utils.exceptions.PasswordsDoNotMatchException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final UserEmailService userEmailService;


    @Transactional
    public void changeUserPassword(User user, ResetPasswordDTO dto) {
        verifyUserConfirmationPassword(dto.password(), dto.confirmPassword());

        setPasswordToUser(user, dto.password());

        log.info("Password successfully changed for user with id: {}", user.getId());
    }

    @Transactional
    public void changeUserPassword(UpdateUserPasswordRequestDTO dto, Authentication auth) {
        User user = userService.findByEmail(auth.getName());

        verifyUserConfirmationPassword(dto.newPassword(), dto.confirmPassword());
        verifyPassword(dto.currentPassword(), user.getPassword());

        setPasswordToUser(user, dto.newPassword());

        userEmailService.sendNotificationAboutPasswordUpdate(user.getEmail());

        log.info("Password successfully changed for user with id: {}", user.getId());
    }

    private void setPasswordToUser(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }

    private static void verifyUserConfirmationPassword(String firstPassword, String secondPassword) {
        if (!firstPassword.equals(secondPassword)) {
            throw new PasswordsDoNotMatchException("Passwords do not match!");
        }
    }

    //todo duplicated in AuthService
    private void verifyPassword(String providedPassword, String expectedPassword) {
        //todo to avoid duplication i think we should decompose services to different use cases. 
        // e.g all logic related to password should be in separate service, 
        if (!passwordEncoder.matches(providedPassword, expectedPassword)) {
            throw new BadCredentialsException("Invalid credentials. Password does not match!");
        }
    }
}
