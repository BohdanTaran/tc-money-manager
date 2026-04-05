package org.tc.mtracker.common.file;

import java.util.Set;

public enum SupportedUploadType {
    JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg"), Set.of("image/jpeg", "image/jpg")),
    PNG("image/png", "png", Set.of("png"), Set.of("image/png")),
    GIF("image/gif", "gif", Set.of("gif"), Set.of("image/gif")),
    WEBP("image/webp", "webp", Set.of("webp"), Set.of("image/webp")),
    PDF("application/pdf", "pdf", Set.of("pdf"), Set.of("application/pdf"));

    private final String mimeType;
    private final String preferredExtension;
    private final Set<String> extensions;
    private final Set<String> mimeTypes;

    SupportedUploadType(String mimeType, String preferredExtension, Set<String> extensions, Set<String> mimeTypes) {
        this.mimeType = mimeType;
        this.preferredExtension = preferredExtension;
        this.extensions = extensions;
        this.mimeTypes = mimeTypes;
    }

    public String mimeType() {
        return mimeType;
    }

    public String preferredExtension() {
        return preferredExtension;
    }

    public boolean matchesExtension(String extension) {
        return extensions.contains(extension);
    }

    public boolean matchesMimeType(String candidateMimeType) {
        return mimeTypes.contains(candidateMimeType);
    }
}
