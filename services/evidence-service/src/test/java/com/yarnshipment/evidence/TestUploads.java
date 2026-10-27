package com.yarnshipment.evidence;

import com.yarnshipment.evidence.domain.model.EvidenceSource;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.StoredObject;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import java.time.Instant;
import java.util.UUID;

/** Testlerde kullanılan hazır yükleme kayıtları. */
public final class TestUploads {

    public static final UUID EVIDENCE_ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000e0001");
    public static final UUID SHIPMENT_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    public static final Instant NOW = Instant.parse("2026-10-27T09:00:00Z");
    public static final long SIZE = 2_048L;
    public static final long TTL_SECONDS = 600L;
    public static final String JPEG = "image/jpeg";
    public static final String SHA = "a".repeat(64);

    private static final long CAPTURED_SECONDS_AGO = 30L;

    private TestUploads() {}

    public static UploadIntent.DeclaredFile declared(EvidenceType type) {
        return new UploadIntent.DeclaredFile(
                type, JPEG, SIZE, EvidenceSource.CAMERA, NOW.minusSeconds(CAPTURED_SECONDS_AGO));
    }

    public static UploadIntent intent(UUID requestedBy) {
        return new UploadIntent(
                EVIDENCE_ID,
                SHIPMENT_ID,
                TestTokens.SITE_ID,
                declared(EvidenceType.DISPATCH_SCALE),
                UploadIntent.storageKeyOf(NOW, SHIPMENT_ID, EVIDENCE_ID),
                requestedBy,
                NOW,
                expiry());
    }

    public static Instant expiry() {
        return NOW.plusSeconds(TTL_SECONDS);
    }

    public static StoredObject stored() {
        return new StoredObject(SIZE, SHA, JPEG);
    }
}
