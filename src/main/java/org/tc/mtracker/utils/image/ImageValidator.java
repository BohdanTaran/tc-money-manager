package org.tc.mtracker.utils.image;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {
    private Set<String> allowedExtensions;
    private Set<String> allowedMimeTypes;

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        this.allowedExtensions = Set.of(constraintAnnotation.allowedExtensions());
        this.allowedMimeTypes = Set.of(constraintAnnotation.allowedMimeTypes());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty() || file.getContentType() == null) {
            return true;
        }

        if (!allowedMimeTypes.contains(file.getContentType())) {
            return false;
        }

        String extension = extractExtension(file.getOriginalFilename());
        return extension != null && allowedExtensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
