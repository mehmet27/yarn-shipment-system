package com.yarnshipment.discrepancy.domain.claim;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestClaims;
import com.yarnshipment.discrepancy.TestComparisons;
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

    private final ClaimTarget target = new ClaimTarget(
            TestComparisons.SHIPMENT,
            TestComparisons.SITE,
            TestComparisons.SUBCONTRACTOR,
            TestComparisons.SHIPMENT_NO,
            FRIDAY,
            TestComparisons.KEYS);

    @Test
    @DisplayName("İtiraz süresi teslimden itibaren 3 iş günüdür; hafta sonu sayılmaz")
    void shouldCountThreeBusinessDays() {
        assertThat(target.deadline()).isEqualTo(WEDNESDAY_NOON);
        assertThat(target.isOpenAt(WEDNESDAY_NOON)).isTrue();
        assertThat(target.isOpenAt(WEDNESDAY_NOON.plusSeconds(1))).isFalse();
    }

    @Test
    @DisplayName("Süre içinde, teslim edilmiş ve itiraz edilmemiş çuval için fotoğraflı itiraz kabul edilir")
    void shouldAcceptValidClaim() {
        ClaimRequest request = new ClaimRequest("B kalite çıktı", List.of(" UK-0001 ", "UK-0001"), PHOTO);

        target.requireClaimable(request, List.of("UK-0002"), FRIDAY);

        assertThat(request.bagKeys()).containsExactly("UK-0001");
    }

    @Test
    @DisplayName("Süresi dolan, teslimde olmayan, daha önce itiraz edilen ya da fotoğrafsız itiraz reddedilir")
    void shouldRejectInvalidClaims() {
        assertThat(rejectionOf(
                        new ClaimRequest("x", List.of("UK-0001"), PHOTO), List.of(), WEDNESDAY_NOON.plusSeconds(1)))
                .isEqualTo(ClaimRejection.WINDOW_CLOSED);
        assertThat(rejectionOf(new ClaimRequest("x", List.of("UK-9999"), PHOTO), List.of(), FRIDAY))
                .isEqualTo(ClaimRejection.BAG_NOT_DELIVERED);
        assertThat(rejectionOf(new ClaimRequest("x", List.of(), PHOTO), List.of(), FRIDAY))
                .isEqualTo(ClaimRejection.BAG_NOT_DELIVERED);
        assertThat(rejectionOf(new ClaimRequest("x", List.of("UK-0001"), PHOTO), List.of("UK-0001"), FRIDAY))
                .isEqualTo(ClaimRejection.BAG_ALREADY_CLAIMED);
        assertThat(rejectionOf(new ClaimRequest("x", List.of("UK-0001"), List.of()), List.of(), FRIDAY))
                .isEqualTo(ClaimRejection.EVIDENCE_REQUIRED);
    }

    private ClaimRejection rejectionOf(ClaimRequest request, List<String> claimed, Instant at) {
        return TestClaims.rejectionOf(() -> target.requireClaimable(request, claimed, at));
    }
}
