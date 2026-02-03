package org.tc.mtracker.image;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {
    private final List<String> allowedExtensions = Arrays.asList("png", "jpg", "jpeg");
    private final List<String> allowedMimeTypes = Arrays.asList("image/png", "image/jpg", "image/jpeg");

    @Override
    public boolean isValid(MultipartFile multipartFile, ConstraintValidatorContext constraintValidatorContext) {
        if (multipartFile == null || multipartFile.isEmpty() || multipartFile.getContentType() == null) {
            return true;
        }
        String contentType = multipartFile.getContentType();

        if (!allowedMimeTypes.contains(contentType)) return false;
        String originalFilename = multipartFile.getOriginalFilename();

        if (originalFilename == null) return false;

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1);

        return allowedExtensions.contains(extension);
    }
}

