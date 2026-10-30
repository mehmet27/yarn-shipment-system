package com.yarnshipment.bag.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Kalitesi kilitlenmiş parti; çuval yalnızca böyle bir partiye üretilir ve kalitesini buradan kopyalar (rapor Bölüm
 * 10).
 *
 * @param bagCount partiye üretilmiş çuval sayısı, iptal edilenler dahil
 */
public record LockedLot(
        UUID lotId,
        UUID siteId,
        String lotNo,
        UUID productId,
        String qualityGradeCode,
        Instant lockedAt,
        int bagCount) {}
