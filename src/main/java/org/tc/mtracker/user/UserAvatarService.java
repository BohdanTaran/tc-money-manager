package org.tc.mtracker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.utils.S3Service;
import org.tc.mtracker.utils.exceptions.FileStorageException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarService {

    private final S3Service s3Service;

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
    }
}
