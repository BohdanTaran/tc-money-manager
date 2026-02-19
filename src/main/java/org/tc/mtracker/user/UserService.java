package org.tc.mtracker.user;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.auth.EmailService;
import org.tc.mtracker.security.CustomUserDetails;
import org.tc.mtracker.security.JwtService;
import org.tc.mtracker.user.dto.RequestUpdateUserEmailDTO;
import org.tc.mtracker.user.dto.ResponseUserProfileDTO;
import org.tc.mtracker.user.dto.UpdateUserProfileDTO;
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
    private final JwtService jwtService;
    private final EmailService emailService;

    public User save(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User with email " + email + " was not found!")
        );
    }

    public boolean isExistsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public ResponseUserProfileDTO updateProfile(UpdateUserProfileDTO dto, MultipartFile avatar, Authentication auth) {
        User user = findByEmail(auth.getName());

        if (avatar != null && !avatar.isEmpty()) {
            uploadAvatar(avatar, user);
        }

        userMapper.updateEntityFromDto(dto, user);

        userRepository.save(user);

        String presignedAvatarUrl = generateAvatarUrl(user);
        log.info("User with id {} is updated successfully!", user.getId());

        return new ResponseUserProfileDTO(user.getFullName(), presignedAvatarUrl);
    }

    @Transactional
    public void updateEmail(RequestUpdateUserEmailDTO dto, Authentication auth) {
        User user = findByEmail(auth.getName());

        if (isExistsByEmail(dto.email())) {
            throw new UserAlreadyExistsException("Email already used");
        }

        user.setPendingEmail(dto.email());
        String generatedToken = jwtService.generateToken(
                Map.of("purpose", "email_update_verification"),
                new CustomUserDetails(user)
        );

        user.setVerificationToken(generatedToken);

        emailService.sendVerificationEmail(dto.email(), generatedToken);

        userRepository.save(user);
    }

    @Transactional
    public void verifyEmailUpdate(String token) {
        String purpose = jwtService.extractClaim(token, claims -> claims.get("purpose", String.class));
        if (!"email_update_verification".equals(purpose)) {
            throw new JwtException("Invalid token type for verification");
        }

        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email).orElseThrow();

        if (user.getVerificationToken() == null || !token.equals(user.getVerificationToken())) {
            throw new EmailVerificationException("Invalid token for verification");
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    public String generateAvatarUrl(User user) {
        return user.getAvatarId() != null ? s3Service.generatePresignedUrl(user.getAvatarId()) : null;
    }

    public String uploadAvatar(MultipartFile avatar, User user) {
        String avatarId = user.getAvatarId();
        if (avatarId == null) {
            avatarId = UUID.randomUUID().toString();
            user.setAvatarId(avatarId);
        }
        try {
            s3Service.saveFile(avatarId, avatar);
            log.info("Avatar with id {} is uploaded successfully!", avatarId);
        } catch (FileStorageException ex) {
            log.error("Error while uploading avatar for user {}: {}", user.getId(), ex.getMessage());
        }
        return generateAvatarUrl(user);
    }

}
