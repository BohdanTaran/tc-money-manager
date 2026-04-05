package org.tc.mtracker.common.file;

import java.util.Locale;
import java.util.Optional;

public final class FileExtensionUtils {

    private FileExtensionUtils() {
    }

    public static Optional<String> extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return Optional.empty();
        }

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
            return Optional.empty();
        }

        return Optional.of(originalFilename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT));
    }
}
