package com.yarnshipment.shipment.domain.model;

import com.yarnshipment.shipment.domain.packaging.PackageType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Çıkışta sevkiyata atanmış çuval.
 *
 * @param bagId çuval kimliği
 * @param uniqueKey çuvalın tekil anahtarı
 * @param packageType ambalaj türü (çuval, koli, palet, bobin)
 * @param lotNo parti numarası
 * @param qualityGradeCode kalite sınıfı
 * @param scanMethod atamanın yapılış yöntemi
 * @param assignedAt atama zamanı
 */
public record DispatchBag(
        UUID bagId,
        String uniqueKey,
        PackageType packageType,
        String lotNo,
        String qualityGradeCode,
        ScanMethod scanMethod,
        Instant assignedAt) {

    public DispatchBag {
        Objects.requireNonNull(bagId, "bagId");
        Objects.requireNonNull(uniqueKey, "uniqueKey");
        Objects.requireNonNull(packageType, "packageType");
        Objects.requireNonNull(lotNo, "lotNo");
        Objects.requireNonNull(qualityGradeCode, "qualityGradeCode");
        Objects.requireNonNull(scanMethod, "scanMethod");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
