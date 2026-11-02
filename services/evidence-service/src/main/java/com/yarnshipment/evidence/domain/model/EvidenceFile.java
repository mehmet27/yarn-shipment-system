package com.yarnshipment.evidence.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Onaylanmış kanıt dosyasının üst verisi. Dosyanın kendisi nesne deposundadır; kayıt silinmez (DOM-17).
 *
 * @param id kanıt kimliği
 * @param shipmentId sevkiyat
 * @param storageKey nesne deposundaki anahtar
 * @param stored depodan okunan gerçek özellikler
 * @param declared bildirilen özellikler
 * @param uploadedAt sunucunun alış zamanı
 * @param uploadedBy yükleyen
 * @param status kayıt durumu
 * @param invalidatedReason geçersiz kılma gerekçesi; yalnızca geçersiz kılınmış kayıtta dolu
 */
public record EvidenceFile(
        UUID id,
        UUID shipmentId,
        String storageKey,
        StoredObject stored,
        UploadIntent.DeclaredFile declared,
        Instant uploadedAt,
        UUID uploadedBy,
        EvidenceStatus status,
        @Nullable String invalidatedReason) {

    public EvidenceFile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(storageKey, "storageKey");
        Objects.requireNonNull(stored, "stored");
        Objects.requireNonNull(declared, "declared");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        Objects.requireNonNull(uploadedBy, "uploadedBy");
        Objects.requireNonNull(status, "status");
    }

    /**
     * Depodaki gerçek dosyadan kanıt kaydı üretir. Boyut ve tür bildirilenle aynı değilse yükleme reddedilir; hash'i
     * her zaman sunucu hesaplar (SEC-09).
     */
    public static EvidenceFile confirm(UploadIntent intent, StoredObject stored, Instant at) {
        if (stored.sizeBytes() != intent.file().sizeBytes()
                || !stored.mimeType().equalsIgnoreCase(intent.file().mimeType())) {
            throw new UploadMismatchException(intent.id());
        }
        return new EvidenceFile(
                intent.id(),
                intent.shipmentId(),
                intent.storageKey(),
                stored,
                intent.file(),
                at,
                intent.requestedBy(),
                EvidenceStatus.VALID,
                null);
    }

    /** Kaydı gerekçeyle geçersiz kılar; dosya ve hash'i yerinde kalır. Bir kez yapılabilir. */
    public EvidenceFile invalidate(String reason) {
        if (reason.isBlank()) {
            throw new InvalidationReasonRequiredException();
        }
        if (status == EvidenceStatus.INVALIDATED) {
            throw new EvidenceAlreadyInvalidatedException(id);
        }
        return new EvidenceFile(
                id,
                shipmentId,
                storageKey,
                stored,
                declared,
                uploadedAt,
                uploadedBy,
                EvidenceStatus.INVALIDATED,
                reason.strip());
    }
}
