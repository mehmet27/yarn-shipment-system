package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.StoredObject;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** S3 uyumlu nesne deposu. */
public interface ObjectStoragePort {

    /**
     * İmzalı yükleme adresi.
     *
     * @param url PUT adresi
     * @param headers yüklemede aynen gönderilecek başlıklar
     * @param expiresAt adresin sona erdiği zaman
     */
    record PresignedUpload(String url, Map<String, String> headers, Instant expiresAt) {

        public PresignedUpload {
            headers = Map.copyOf(headers);
        }
    }

    /**
     * Yükleme sırasında doğrulanacak özellikler.
     *
     * @param storageKey nesne anahtarı
     * @param mimeType dosya türü
     * @param sizeBytes beklenen boyut
     */
    record UploadTarget(String storageKey, String mimeType, long sizeBytes) {}

    /** Adres, depo ayarındaki süre kadar geçerlidir (ör. 10 dakika). */
    PresignedUpload presignUpload(UploadTarget target);

    /** Nesneyi okuyup gerçek boyut, tür ve SHA-256'sını hesaplar; nesne yoksa boş döner. */
    Optional<StoredObject> inspect(String storageKey);
}
