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

    public UserResponseDTO getUser(Authentication auth) {
        User user = findByEmail(auth.getName());

        return userMapper.toDto(user);
    }

    @Transactional
    public UserProfileResponseDTO updateProfile(UpdateUserProfileRequestDTO dto, MultipartFile avatar, Authentication auth) {
        User user = findByEmail(auth.getName());

        userAvatarService.uploadAvatar(avatar, user);
        userMapper.updateEntityFromDto(dto, user);
        userRepository.save(user);

        log.info("User with id {} is updated successfully!", user.getId());
        return userMapper.toUserProfileResponseDTO(user);
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

        userEmailService.verifyEmailUpdate(user, token);
    }

}
