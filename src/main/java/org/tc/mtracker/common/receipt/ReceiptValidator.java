package org.tc.mtracker.common.receipt;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public class ReceiptValidator implements ConstraintValidator<ValidReceiptFile, MultipartFile> {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "pdf");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpg", "image/jpeg", "application/pdf");

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty() || file.getContentType() == null) return true;

        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) return false;

        String ext = extractExtension(file.getOriginalFilename());
        return ext != null && ALLOWED_EXTENSIONS.contains(ext);
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
