package com.yarnshipment.bag.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Kalitesi kilitlenmiş parti; çuvalın partisi ve kalitesi buradan gelir. Kilit geri alınamaz.
 *
 * @param lotId parti
 * @param siteId partinin lokasyonu
 * @param lotNo parti numarası
 * @param productId iplik
 * @param qualityGradeCode kalite sınıfı kodu
 * @param lockedAt kilitlenme zamanı
 */
public record LotQualityLock(
        UUID lotId, UUID siteId, String lotNo, UUID productId, String qualityGradeCode, Instant lockedAt) {

    public LotQualityLock {
        Objects.requireNonNull(lotId, "lotId");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(lotNo, "lotNo");
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(qualityGradeCode, "qualityGradeCode");
        Objects.requireNonNull(lockedAt, "lockedAt");
    }
}
