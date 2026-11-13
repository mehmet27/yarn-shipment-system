package com.yarnshipment.evidence.adapter.out.storage;

import java.time.Instant;

/**
 * İmzalı adresin taşıdığı izin: hangi işlem, hangi dosya, hangi tür ve boyut, ne zamana kadar.
 *
 * @param operation yükleme ya da indirme
 * @param storageKey dosya anahtarı
 * @param mimeType yüklemede beklenen dosya türü
 * @param sizeBytes yüklemede beklenen boyut
 * @param expiresAt adresin sona erdiği zaman
 */
record FileGrant(Operation operation, String storageKey, String mimeType, long sizeBytes, Instant expiresAt) {

    /** İzin verilen işlem. */
    enum Operation {
        UPLOAD,
        DOWNLOAD
    }
}
