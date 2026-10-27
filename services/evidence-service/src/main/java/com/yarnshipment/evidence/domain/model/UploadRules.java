package com.yarnshipment.evidence.domain.model;

import java.util.Set;

/** Yüklenebilecek dosyaların sınırları. */
public final class UploadRules {

    public static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> MIME_TYPES = Set.of("image/jpeg", "image/png", "image/heic", "application/pdf");

    private UploadRules() {}

    public static boolean isAllowedMimeType(String mimeType) {
        return MIME_TYPES.contains(mimeType);
    }

    public static boolean isAllowedSize(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= MAX_SIZE_BYTES;
    }
}
