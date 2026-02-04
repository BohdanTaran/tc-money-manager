package org.tc.mtracker.user.image;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpg", "image/jpeg");

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            return true;
        }

        String contentType = file.getContentType();
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            return false;
        }

        String extension = extractExtension(file.getOriginalFilename());
        return extension != null && ALLOWED_EXTENSIONS.contains(extension);
    }

    private static String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
            return null;
        }

        return originalFilename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
