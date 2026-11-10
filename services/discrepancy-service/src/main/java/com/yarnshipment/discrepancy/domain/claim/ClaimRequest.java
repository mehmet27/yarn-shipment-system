package com.yarnshipment.discrepancy.domain.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * İtirazı açanın girdiği bilgiler.
 *
 * @param description itirazın açıklaması
 * @param claimedKg itiraz edilen kilo; sıfırdan büyük, en çok üç ondalık (kilogram bazlı itiraz, A5)
 * @param fabricReceiptId isteğe bağlı kumaş girişi kimliği (kumaş girişi henüz yok)
 * @param evidenceIds itiraz fotoğrafları ({@code QUALITY_CLAIM} kanıtları)
 */
public record ClaimRequest(
        String description, BigDecimal claimedKg, @Nullable UUID fabricReceiptId, List<UUID> evidenceIds) {

    private static final int KG_SCALE = 3;

    public ClaimRequest {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(claimedKg, "claimedKg");
        if (claimedKg.signum() <= 0 || claimedKg.stripTrailingZeros().scale() > KG_SCALE) {
            throw new ClaimRejectedException(ClaimRejection.CLAIMED_KG_INVALID, "claimedKg=" + claimedKg);
        }
        claimedKg = claimedKg.setScale(KG_SCALE, RoundingMode.UNNECESSARY);
        evidenceIds = evidenceIds.stream().distinct().toList();
    }
}
