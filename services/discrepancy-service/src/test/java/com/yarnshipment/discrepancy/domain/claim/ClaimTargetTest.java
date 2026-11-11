package com.yarnshipment.discrepancy.domain.claim;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestClaims;
import com.yarnshipment.discrepancy.TestComparisons;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClaimTargetTest {

    /** Cuma 12:00 (İstanbul); 3 iş günü sonrası çarşamba 12:00. */
    private static final Instant FRIDAY = Instant.parse("2026-10-23T09:00:00Z");

    private static final Instant WEDNESDAY_NOON = Instant.parse("2026-10-28T09:00:00Z");
    private static final List<UUID> PHOTO = List.of(UUID.fromString("0192f0a0-0000-7000-8000-00000000e001"));
    private static final BigDecimal NET_KG = new BigDecimal("2000.000");

    private final ClaimTarget target = new ClaimTarget(
            TestComparisons.SHIPMENT,
            TestComparisons.SITE,
            TestComparisons.SUBCONTRACTOR,
            TestComparisons.SHIPMENT_NO,
            FRIDAY,
            NET_KG);

    @Test
    @DisplayName("Hedef tarih 3 iş günüdür (hafta sonu sayılmaz) ama itiraz süresi sınırsızdır")
    void shouldNotLimitClaimWindow() {
        assertThat(target.deadline()).isEqualTo(WEDNESDAY_NOON);
        assertThat(target.isOpenAt(WEDNESDAY_NOON)).isTrue();
        assertThat(target.isOpenAt(WEDNESDAY_NOON.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("Süre içinde, kilosu aşmayan ve fotoğraflı itiraz kabul edilir; kilo üç ondalığa tamamlanır")
    void shouldAcceptValidClaim() {
        ClaimRequest request = new ClaimRequest("B kalite çıktı", new BigDecimal("2000"), null, PHOTO);

        target.requireClaimable(request, FRIDAY);

        assertThat(request.claimedKg()).isEqualTo(new BigDecimal("2000.000"));
    }

    @Test
    @DisplayName("Sevkiyat kilosunu aşan ya da fotoğrafsız itiraz reddedilir; geç kalmak red nedeni değildir")
    void shouldRejectInvalidClaims() {
        target.requireClaimable(request("10", PHOTO), WEDNESDAY_NOON.plusSeconds(1));
        assertThat(rejectionOf(request("2000.001", PHOTO), FRIDAY))
                .isEqualTo(ClaimRejection.CLAIM_EXCEEDS_SHIPMENT_WEIGHT);
        assertThat(rejectionOf(request("10", List.of()), FRIDAY)).isEqualTo(ClaimRejection.EVIDENCE_REQUIRED);
    }

    @Test
    @DisplayName("Sıfır, negatif ya da üçten fazla ondalıklı kilo geçersizdir")
    void shouldRejectInvalidKg() {
        for (String kg : List.of("0", "-5", "10.0001")) {
            assertThat(TestClaims.rejectionOf(() -> request(kg, PHOTO))).isEqualTo(ClaimRejection.CLAIMED_KG_INVALID);
        }
    }

    private static ClaimRequest request(String kg, List<UUID> evidence) {
        return new ClaimRequest("x", new BigDecimal(kg), null, evidence);
    }

    private ClaimRejection rejectionOf(ClaimRequest request, Instant at) {
        return TestClaims.rejectionOf(() -> target.requireClaimable(request, at));
    }
}
