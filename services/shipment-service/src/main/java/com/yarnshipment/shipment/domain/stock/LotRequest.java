package com.yarnshipment.shipment.domain.stock;

import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Girişte yazılan parti numarasının partiye çevrilmesi isteği.
 *
 * @param actorId girişi yapan kullanıcı
 * @param siteIds kullanıcının lokasyonları; parti bunlarda aranır, yeni parti ilkinde açılır
 * @param productId iplik tanımı
 * @param supplierId tedarikçi tanımı
 * @param lotNo normalleştirilmiş parti numarası
 * @param qualityGradeId yeni parti için kalite sınıfı
 */
public record LotRequest(
        UUID actorId,
        Set<UUID> siteIds,
        UUID productId,
        UUID supplierId,
        String lotNo,
        @Nullable UUID qualityGradeId) {}
