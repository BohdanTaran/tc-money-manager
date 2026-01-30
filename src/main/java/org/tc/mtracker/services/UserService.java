package org.tc.mtracker.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tc.mtracker.entity.UserEntity;
import org.tc.mtracker.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserEntity save(UserEntity userEntity) {
        return userRepository.save(userEntity);
    }

    boolean isExistsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
