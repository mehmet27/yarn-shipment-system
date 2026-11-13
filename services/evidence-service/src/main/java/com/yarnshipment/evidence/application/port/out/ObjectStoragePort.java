package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.FileContent;
import com.yarnshipment.evidence.domain.model.StoredObject;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Kanıt dosyalarının deposu (sunucu diskinde klasör); yükleme ve indirme adreslerini uygulama imzalar. */
public interface ObjectStoragePort {

    /**
     * İmzalı yükleme adresi.
     *
     * @param url PUT adresi (uygulamanın kendi adresi)
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

    /**
     * İmzalı indirme adresi.
     *
     * @param url GET adresi
     * @param expiresAt adresin sona erdiği zaman
     */
    record PresignedDownload(String url, Instant expiresAt) {}

    /** Adres, depo ayarındaki süre kadar geçerlidir (ör. 10 dakika). */
    PresignedUpload presignUpload(UploadTarget target);

    /** Nesneyi okuyup gerçek boyut, tür ve SHA-256'sını hesaplar; nesne yoksa boş döner. */
    Optional<StoredObject> inspect(String storageKey);

    /** Nesneyi kısa süre okuyabilecek imzalı adres; süre depo ayarındadır. */
    PresignedDownload presignDownload(String storageKey);

    /** Nesnenin baytlarını indirir; nesne yoksa boş döner. Boyut yüklemede 20 MB ile sınırlıdır. */
    Optional<byte[]> download(String storageKey);

    /**
     * İmzalı yükleme adresine gelen baytları kaydeder. İmza, süre, boyut ve tür doğrulanır; dosya türü ilk
     * baytlardan da denetlenir. Var olan dosyanın üzerine yazılmaz; aynı adrese tekrar gönderim sessizce kabul edilir.
     *
     * @param contentLength istemcinin bildirdiği boyut; bildirmediyse negatif
     */
    void receiveUpload(String token, @Nullable String contentType, long contentLength, InputStream body);

    /** İmzalı indirme adresindeki dosyayı açar; akışı çağıran kapatır. */
    FileContent openDownload(String token);
}
