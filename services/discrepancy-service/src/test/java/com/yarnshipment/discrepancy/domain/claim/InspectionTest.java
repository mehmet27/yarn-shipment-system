package com.yarnshipment.discrepancy.domain.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InspectionTest {

    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final UUID DOCUMENT = UUID.fromString("0192f0a0-0000-7000-8000-00000000e002");
    private static final Instant AT = Instant.parse("2026-10-24T09:00:00Z");

    @Test
    @DisplayName("Lab sonucu belgeyle girilir; numune sonucu belgesiz de girilebilir")
    void shouldRequireDocumentForLab() {
        assertThatThrownBy(() -> pending(InspectionKind.LAB).recordResult(result(null)))
                .isInstanceOfSatisfying(
                        ClaimRejectedException.class,
                        exception ->
                                assertThat(exception.rejection()).isEqualTo(ClaimRejection.RESULT_EVIDENCE_REQUIRED));

        assertThat(pending(InspectionKind.LAB).recordResult(result(DOCUMENT)).isPending())
                .isFalse();
        assertThat(pending(InspectionKind.SAMPLE).recordResult(result(null)).result())
                .isNotNull();
    }

    @Test
    @DisplayName("Sonucu girilmiş kontrole ikinci sonuç girilemez")
    void shouldRejectSecondResult() {
        Inspection recorded = pending(InspectionKind.SAMPLE).recordResult(result(null));

        assertThatThrownBy(() -> recorded.recordResult(result(null)))
                .isInstanceOfSatisfying(
                        ClaimRejectedException.class,
                        exception -> assertThat(exception.rejection()).isEqualTo(ClaimRejection.INSPECTION_RECORDED));
    }

    private static Inspection pending(InspectionKind kind) {
        return new Inspection(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                kind,
                new Inspection.Request(null, ACTOR, AT),
                null);
    }

    private static Inspection.Result result(@Nullable UUID evidenceId) {
        return new Inspection.Result("şahit numuneyle aynı", evidenceId, ACTOR, AT);
    }
}
