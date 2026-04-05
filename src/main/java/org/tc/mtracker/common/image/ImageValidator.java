package org.tc.mtracker.common.image;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.file.UploadFilePolicies;
import org.tc.mtracker.common.file.UploadValidation;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        return UploadValidation.isAllowed(file, UploadFilePolicies.AVATAR_TYPES);
    }
}
