package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Kalite itirazı okuma ve yazımları. */
public interface QualityClaimPort {

    /** Karşılaştırması yapılmış ve lokasyon ile fasoncu bilinen teslim; yoksa boş. */
    Optional<ClaimTarget> findTarget(UUID shipmentId);

    /** Sevkiyatın itirazları, açılış sırasıyla. */
    List<QualityClaim> findClaims(UUID shipmentId);

    Optional<QualityClaim> findClaim(UUID claimId);

    /**
     * {@code QUALITY} uyuşmazlığını, itirazı, çuvallarını, açılış ve kanıt geçmişini ve {@code DiscrepancyOpened}
     * olayını aynı transaction'da yazar (EVT-03).
     */
    QualityClaim saveClaim(NewQualityClaim claim);
}
