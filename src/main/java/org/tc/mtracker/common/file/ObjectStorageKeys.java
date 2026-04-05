package org.tc.mtracker.common.file;

import java.util.UUID;

public final class ObjectStorageKeys {

    private static final String AVATARS_PREFIX = "avatars/";
    private static final String RECEIPTS_PREFIX = "receipts/";

    private ObjectStorageKeys() {
    }

    public static String newAvatarKey() {
        return AVATARS_PREFIX + UUID.randomUUID();
    }

    public static String receiptKey(UUID receiptId) {
        return RECEIPTS_PREFIX + receiptId;
    }
}
