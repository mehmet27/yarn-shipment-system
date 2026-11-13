package com.yarnshipment.discrepancy.adapter.out.persistence;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.out.NewQualityClaim;
import com.yarnshipment.discrepancy.domain.claim.ClaimRequest;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Gerçek şemaya tamamlanmış bir sevkiyat izi ve 120,5 kg için fotoğraflı bir kalite itirazı yazar. */
final class SeededClaims {

    static final UUID ACTOR = SeededDiscrepancies.ACTOR;
    static final UUID PHOTO = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0003");
    static final Instant OPENED_AT = Instant.parse("2026-10-26T09:00:00Z");
    private static final long DAY = 86_400L;
    private static final long HALF_DAY = 43_200L;
    private static final long LATE_BY = 30 * DAY;
    private static final Instant DUE = Instant.parse("2026-11-02T09:00:00Z");

    private SeededClaims() {}

    static void complete(ShipmentWatchAdapter watch) {
        watch.recordCreated(TestComparisons.watched(), TestComparisons.RECEIVED_AT.minusSeconds(DAY));
        Instant dispatched = TestComparisons.RECEIVED_AT.minusSeconds(HALF_DAY);
        watch.recordDispatched(TestComparisons.SHIPMENT, dispatched, TestComparisons.DISPATCH_KG);
        watch.recordCompleted(TestComparisons.SHIPMENT, TestComparisons.RECEIVED_AT);
    }

    static QualityClaim open(QualityClaimAdapter claims) {
        ClaimTarget target = claims.findTarget(TestComparisons.SHIPMENT).orElseThrow();
        return claims.saveClaim(new NewQualityClaim(
                target,
                new ClaimRequest("B kalite çıktı", new BigDecimal("120.5"), null, List.of(PHOTO)),
                ACTOR,
                OPENED_AT,
                DUE));
    }

    /** Hedef tarihten 30 gün sonra açılan itiraz; süre sınırı olmadığı için yazılabilmelidir. */
    static QualityClaim openAfterTarget(QualityClaimAdapter claims) {
        ClaimTarget target = claims.findTarget(TestComparisons.SHIPMENT).orElseThrow();
        return claims.saveClaim(new NewQualityClaim(
                target,
                new ClaimRequest("Geç açıldı", new BigDecimal("10"), null, List.of(PHOTO)),
                ACTOR,
                target.deadline().plusSeconds(LATE_BY),
                target.deadline()));
    }
}
