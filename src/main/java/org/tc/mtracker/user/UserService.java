package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.tc.mtracker.user.dto.UpdateUserProfileDTO;
import org.tc.mtracker.utils.exceptions.UserNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    public boolean isExistsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public boolean updateProfile(UpdateUserProfileDTO dto, Authentication auth) {
        User user = null;
        try {
             user = userRepository.findByEmail(auth.getName()).orElseThrow(
                    () -> new UserNotFoundException("User was not found!")
            );

            user.setFullName(dto.fullName());

            userRepository.save(user);
        } catch (Exception e) {
            return false;
        }

        log.info("User with id {} is updated successfully!", user.getId());
        return true;
    }
}
