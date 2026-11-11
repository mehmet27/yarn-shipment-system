package com.yarnshipment.bag.domain.model;

import java.util.UUID;

/**
 * Çuval. Kalite partiden kopyalanır, elle girilmez.
 */
public record Bag(
        UUID id,
        UUID siteId,
        String uniqueKey,
        PackageType packageType,
        String lotNo,
        String qualityGradeCode,
        BagStatus status) {}
