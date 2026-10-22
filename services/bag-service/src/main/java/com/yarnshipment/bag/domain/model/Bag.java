package com.yarnshipment.bag.domain.model;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Çuval. Kalite partiden kopyalanır, elle girilmez.
 *
 * @param reservedShipmentId çuval bir sevkiyata ayrılmışsa o sevkiyat
 */
public record Bag(
        UUID id,
        UUID siteId,
        String uniqueKey,
        PackageType packageType,
        String lotNo,
        String qualityGradeCode,
        BagStatus status,
        @Nullable UUID reservedShipmentId) {}
