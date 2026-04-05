package org.tc.mtracker.common.receipt;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import org.tc.mtracker.common.file.UploadFilePolicies;
import org.tc.mtracker.common.file.UploadValidation;

public class ReceiptValidator implements ConstraintValidator<ValidReceiptFile, MultipartFile> {

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        return UploadValidation.isAllowed(file, UploadFilePolicies.RECEIPT_TYPES);
    }
}
