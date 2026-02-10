package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.user.dto.UpdateUserProfileDTO;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.exceptions.UserNotFoundException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    public boolean isExistsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public void updateProfile(UpdateUserProfileDTO dto, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(
                () -> new UserNotFoundException("User was not found!")
        );

        user.setFullName(dto.fullName());

        userRepository.save(user);

        log.info("User with id {} is updated successfully!", user.getId());
    }

    @Transactional
    public String uploadAvatar(MultipartFile avatar, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow(
                () -> new UserNotFoundException("User was not found!")
        );

        String avatarId = user.getAvatarId();
        if (avatarId == null) {
            avatarId = UUID.randomUUID().toString();
            user.setAvatarId(avatarId);
            userRepository.save(user);
        }
        s3Service.saveFile(avatarId, avatar);

        log.info("Avatar with id {} is uploaded successfully!", avatarId);
        return s3Service.generatePresignedUrl(avatarId);
    }

}
