package com.yarnshipment.discrepancy;

import com.yarnshipment.discrepancy.domain.claim.ClaimRejectedException;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import com.yarnshipment.discrepancy.domain.claim.ClaimTarget;
import com.yarnshipment.discrepancy.domain.claim.QualityClaim;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Testlerde kullanılan örnek itiraz hedefi ve itiraz; lokasyonu {@link TestTokens#SITE_ID}. */
public final class TestClaims {

    public static final UUID CLAIM_ID = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0001");
    public static final UUID PHOTO = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0003");
    public static final UUID OTHER_DOCUMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0004");
    public static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0005");
    /** Tamamlanma cuma; itiraz süresi çarşamba 12:00'ye (İstanbul) kadar. */
    public static final Instant RECEIVED_AT = TestComparisons.RECEIVED_AT;

    public static final Instant WITHIN_WINDOW = Instant.parse("2026-10-26T09:00:00Z");

    private TestClaims() {}

    public static ClaimTarget target() {
        return new ClaimTarget(
                TestDiscrepancies.SHIPMENT,
                TestTokens.SITE_ID,
                TestTokens.SUBCONTRACTOR_ID,
                TestComparisons.SHIPMENT_NO,
                RECEIVED_AT,
                TestComparisons.DISPATCH_KG,
                BigDecimal.ZERO);
    }

    public static QualityClaim claim() {
        return new QualityClaim(
                CLAIM_ID,
                TestDiscrepancies.ID,
                TestDiscrepancies.SHIPMENT,
                TestTokens.SUBCONTRACTOR_ID,
                new QualityClaim.Opening(ACTOR, WITHIN_WINDOW, target().deadline()),
                "B kalite çıktı",
                new BigDecimal("120.500"),
                null,
                List.of(PHOTO));
    }

    /** Çağrının hangi itiraz kuralıyla reddedildiği; reddedilmezse test düşer. */
    public static ClaimRejection rejectionOf(Runnable call) {
        try {
            call.run();
        } catch (ClaimRejectedException exception) {
            return exception.rejection();
        }
        throw new AssertionError("Call was not rejected");
    }
}
