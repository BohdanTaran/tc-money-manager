package org.tc.mtracker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tc.mtracker.entity.User;
import org.tc.mtracker.repository.UserRepository;

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
}
