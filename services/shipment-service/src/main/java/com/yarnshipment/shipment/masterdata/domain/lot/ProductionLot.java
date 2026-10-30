package com.yarnshipment.shipment.masterdata.domain.lot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Parti. Kalite kilitlenince yalnızca etkinlik durumu değişebilir (rapor Bölüm 10).
 *
 * @param id kimlik
 * @param siteId üretildiği lokasyon (GEN-07)
 * @param details numara, iplik, kalite ve üretim tarihi
 * @param labels ekranda gösterilen lokasyon, iplik ve kalite adları
 * @param qualityLockedAt kalitenin kilitlendiği an; kilitsizse boş
 * @param active listelerde görünür mü
 */
public record ProductionLot(
        UUID id,
        UUID siteId,
        LotDetails details,
        Labels labels,
        @Nullable Instant qualityLockedAt,
        boolean active) {

    public ProductionLot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(labels, "labels");
    }

    public boolean isQualityLocked() {
        return qualityLockedAt != null;
    }

    /** Kilitsiz parti düzenlenebilir; kilitliyse {@link LotQualityLockedException}. */
    public void requireUnlocked() {
        if (isQualityLocked()) {
            throw new LotQualityLockedException(id);
        }
    }

    /**
     * Partinin bağlı olduğu kayıtların görünen adları.
     *
     * @param siteName lokasyon adı
     * @param productCode iplik kodu
     * @param productName iplik adı
     * @param qualityGradeCode kalite kodu; kilitte çuvala kopyalanır
     * @param qualityGradeName kalite adı
     */
    public record Labels(
            String siteName, String productCode, String productName, String qualityGradeCode, String qualityGradeName) {

        public Labels {
            Objects.requireNonNull(siteName, "siteName");
            Objects.requireNonNull(productCode, "productCode");
            Objects.requireNonNull(productName, "productName");
            Objects.requireNonNull(qualityGradeCode, "qualityGradeCode");
            Objects.requireNonNull(qualityGradeName, "qualityGradeName");
        }
    }
}
