package com.yarnshipment.evidence.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Onay bekleyen yükleme. İstemci dosyayı kısa ömürlü imzalı adrese yükler, sonra onaylar.
 *
 * @param id kanıt kimliği; onaylanınca kanıt kaydının kimliği olur
 * @param shipmentId sevkiyat
 * @param siteId sevkiyatın çıkış lokasyonu
 * @param file yüklenecek dosyanın bildirilen özellikleri
 * @param storageKey nesne deposundaki anahtar
 * @param requestedBy isteyen; yalnızca o onaylayabilir
 * @param requestedAt istek zamanı
 * @param expiresAt imzalı adresin sona erdiği zaman
 */
public record UploadIntent(
        UUID id,
        UUID shipmentId,
        UUID siteId,
        DeclaredFile file,
        String storageKey,
        UUID requestedBy,
        Instant requestedAt,
        Instant expiresAt) {

    public UploadIntent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(storageKey, "storageKey");
        Objects.requireNonNull(requestedBy, "requestedBy");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /** Nesne deposu anahtarı: {@code evidence/{yıl}/{ay}/{sevkiyat}/{kanıt}} (veritabanı tasarımı). */
    public static String storageKeyOf(Instant at, UUID shipmentId, UUID evidenceId) {
        java.time.ZonedDateTime utc = at.atZone(java.time.ZoneOffset.UTC);
        return "evidence/%04d/%02d/%s/%s".formatted(utc.getYear(), utc.getMonthValue(), shipmentId, evidenceId);
    }

    /**
     * İstemcinin bildirdiği dosya özellikleri.
     *
     * @param type kanıt türü
     * @param mimeType dosya türü
     * @param sizeBytes bayt cinsinden boyut
     * @param source kamera ya da galeri
     * @param capturedAt cihazın bildirdiği çekim zamanı; karar kurallarında kullanılmaz (DOM-11)
     */
    public record DeclaredFile(
            EvidenceType type,
            String mimeType,
            long sizeBytes,
            EvidenceSource source,
            @Nullable Instant capturedAt) {

        public DeclaredFile {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(mimeType, "mimeType");
            Objects.requireNonNull(source, "source");
        }
    }
}
