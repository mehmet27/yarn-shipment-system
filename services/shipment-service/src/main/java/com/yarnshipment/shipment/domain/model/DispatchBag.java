package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Çıkışta sevkiyata atanmış çuval.
 *
 * @param bagId çuval kimliği
 * @param uniqueKey çuvalın tekil anahtarı
 * @param lotNo parti numarası
 * @param qualityGradeCode kalite sınıfı
 * @param scanMethod atamanın yapılış yöntemi
 * @param assignedAt atama zamanı
 */
public record DispatchBag(
        UUID bagId,
        String uniqueKey,
        String lotNo,
        String qualityGradeCode,
        ScanMethod scanMethod,
        Instant assignedAt) {

    public DispatchBag {
        Objects.requireNonNull(bagId, "bagId");
        Objects.requireNonNull(uniqueKey, "uniqueKey");
        Objects.requireNonNull(lotNo, "lotNo");
        Objects.requireNonNull(qualityGradeCode, "qualityGradeCode");
        Objects.requireNonNull(scanMethod, "scanMethod");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
