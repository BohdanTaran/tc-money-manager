package org.tc.mtracker.common.file;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Set;

public final class UploadValidation {

    private UploadValidation() {
    }

    public static boolean isAllowed(MultipartFile file, Set<SupportedUploadType> allowedTypes) {
        if (file == null || file.isEmpty()) {
            return true;
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        Optional<String> extension = FileExtensionUtils.extractExtension(file.getOriginalFilename());
        if (extension.isEmpty()) {
            return false;
        }

        return allowedTypes.stream()
                .anyMatch(type -> type.matchesMimeType(contentType) && type.matchesExtension(extension.get()));
    }

    public static String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return (contentType == null || contentType.isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : contentType;
    }
}
