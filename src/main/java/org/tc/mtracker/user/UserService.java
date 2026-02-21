package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.utils.EmailService;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtPurpose;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.dto.UpdateUserEmailRequestDTO;
import org.tc.mtracker.user.dto.UserProfileResponseDTO;
import org.tc.mtracker.user.dto.UpdateUserProfileRequestDTO;
import org.tc.mtracker.user.dto.UserMapper;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.exceptions.EmailVerificationException;
import org.tc.mtracker.utils.exceptions.FileStorageException;
import org.tc.mtracker.utils.exceptions.UserAlreadyExistsException;
import org.tc.mtracker.utils.exceptions.UserNotFoundException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final UserAvatarService userAvatarService;
    private final UserEmailService userEmailService;
    private final JwtService jwtService;

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User with email " + email + " was not found!")
        );
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public UserProfileResponseDTO updateProfile(UpdateUserProfileRequestDTO dto, MultipartFile avatar, Authentication auth) {
        User user = findByEmail(auth.getName());

        uploadAvatar(avatar, user);

        if (dto != null) {
            userMapper.updateEntityFromDto(dto, user);
        }

        userRepository.save(user);

        String presignedAvatarUrl = generateAvatarUrl(user);
        log.info("User with id {} is updated successfully!", user.getId());

        return new UserProfileResponseDTO(user.getFullName(), presignedAvatarUrl);
    }

    @Transactional
    public void updateEmail(UpdateUserEmailRequestDTO dto, Authentication auth) {
        if (existsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("Email already used");
        }

        User user = findByEmail(auth.getName());

        userEmailService.sendEmailUpdateVerification(user, dto.email());
    }

    @Transactional
    public void verifyEmailUpdate(String token) {
        String email = jwtService.extractUsername(token);
        User user = findByEmail(email);

    public String generateAvatarUrl(User user) {
        return user.getAvatarId() != null ? s3Service.generatePresignedUrl(user.getAvatarId()) : null;
    }

    public void uploadAvatar(MultipartFile avatar, User user) {
        if (avatar == null || avatar.isEmpty()) {
            return;
        }
        String avatarId = user.getAvatarId();
        if (avatarId == null) {
            avatarId = UUID.randomUUID().toString();
        }
        try {
            s3Service.saveFile(avatarId, avatar);
            user.setAvatarId(avatarId);
            log.info("Avatar with id {} is uploaded successfully for user {}!", avatarId, user.getId());
        } catch (FileStorageException ex) {
            log.error("Error while uploading avatar for user {}: {}", user.getId(), ex.getMessage());
        }
        userEmailService.verifyEmailUpdate(user, token);
    }

}
