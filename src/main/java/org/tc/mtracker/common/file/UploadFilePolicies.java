package org.tc.mtracker.common.file;

import java.util.EnumSet;
import java.util.Set;

public final class UploadFilePolicies {

    public static final Set<SupportedUploadType> AVATAR_TYPES =
            EnumSet.of(SupportedUploadType.JPEG, SupportedUploadType.PNG, SupportedUploadType.GIF, SupportedUploadType.WEBP);

    public static final Set<SupportedUploadType> RECEIPT_TYPES =
            EnumSet.of(SupportedUploadType.JPEG, SupportedUploadType.PNG, SupportedUploadType.WEBP, SupportedUploadType.PDF);

    private UploadFilePolicies() {
    }
}
