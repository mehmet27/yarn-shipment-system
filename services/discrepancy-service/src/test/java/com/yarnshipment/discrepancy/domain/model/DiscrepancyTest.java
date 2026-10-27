package com.yarnshipment.discrepancy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.discrepancy.TestDiscrepancies;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscrepancyTest {

    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final Instant AT = Instant.parse("2026-10-24T09:00:00Z");

    @Test
    @DisplayName("Atama sorumluyu yazar, incelemeye alır ve vadeyi korur")
    void shouldAssignAndMoveToReview() {
        Discrepancy assigned = TestDiscrepancies.open(SITE).assignTo(USER);

        assertThat(assigned.status()).isEqualTo(DiscrepancyStatus.IN_REVIEW);
        assertThat(assigned.handling().assigneeId()).isEqualTo(USER);
        assertThat(assigned.handling().dueAt()).isEqualTo(TestDiscrepancies.DUE_AT);
    }

    @Test
    @DisplayName("Çözülmüş uyuşmazlık atanamaz ve tekrar çözülemez")
    void shouldRejectChangesAfterResolution() {
        Discrepancy resolved = TestDiscrepancies.open(SITE).resolve(decision(Resolution.TOLERANCE, null));

        assertThat(resolved.status()).isEqualTo(DiscrepancyStatus.RESOLVED);
        assertThatThrownBy(() -> resolved.assignTo(USER)).isInstanceOf(DiscrepancyNotOpenException.class);
        assertThatThrownBy(() -> resolved.resolve(decision(Resolution.DATA_ERROR, null)))
                .isInstanceOf(DiscrepancyNotOpenException.class);
    }

    @Test
    @DisplayName("Sorumluluk atayan karar sorumlu taraf olmadan verilemez")
    void shouldRequireLiablePartyForLiabilityDecision() {
        Discrepancy open = TestDiscrepancies.open(SITE);

        assertThatThrownBy(() -> open.resolve(decision(Resolution.LIABILITY_ASSIGNED, null)))
                .isInstanceOf(LiablePartyRequiredException.class);
        Discrepancy.Decision decision = open.resolve(decision(Resolution.LIABILITY_ASSIGNED, "Nakliyeci"))
                .decision();
        assertThat(decision).isNotNull();
        assertThat(Objects.requireNonNull(decision).liableParty()).isEqualTo("Nakliyeci");
    }

    private static Discrepancy.Decision decision(Resolution resolution, @Nullable String liableParty) {
        return new Discrepancy.Decision(resolution, "not", liableParty, USER, AT);
    }
}
