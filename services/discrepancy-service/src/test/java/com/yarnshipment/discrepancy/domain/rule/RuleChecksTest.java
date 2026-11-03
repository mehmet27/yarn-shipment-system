package com.yarnshipment.discrepancy.domain.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.discrepancy.domain.model.DiscrepancyType;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.model.InvalidSettingException;
import com.yarnshipment.discrepancy.domain.model.Severity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Karşılaştırma dışı kurallar: zorunlu ve tekrar kullanılan fotoğraf, yolda süre aşımı. */
class RuleChecksTest {

    private static final Instant DISPATCHED = Instant.parse("2026-10-23T08:00:00Z");
    private static final int ELAPSED_HOURS = 30;
    private static final Duration THIRTY_HOURS_AND_A_BIT =
            Duration.ofHours(ELAPSED_HOURS).plusMinutes(5);

    @Test
    @DisplayName("Eksik her zorunlu çıkış fotoğrafı ayrı PHOTO_MISSING (Sarı) olur; teslim türleri sayılmaz")
    void shouldFindMissingPhotos() {
        List<Finding> findings = EvidenceChecks.missing(Set.of("BAG_OVERVIEW", "DELIVERY_SCALE"));

        assertThat(findings)
                .extracting(Finding::expectedValue)
                .containsExactly("DISPATCH_SCALE", "VEHICLE_PLATE", "SEAL");
        assertThat(findings).allSatisfy(finding -> {
            assertThat(finding.type()).isEqualTo(DiscrepancyType.PHOTO);
            assertThat(finding.severity()).isEqualTo(Severity.YELLOW);
            assertThat(finding.ruleCode()).isEqualTo("PHOTO_MISSING");
        });
        assertThat(EvidenceChecks.missing(EvidenceChecks.REQUIRED_AT_DISPATCH)).isEmpty();
    }

    @Test
    @DisplayName("Tekrar kullanılan fotoğraf Kırmızıdır; değerler yalnızca kanıt kimlikleridir")
    void shouldFlagReusedPhoto() {
        UUID original = UUID.randomUUID();
        UUID copy = UUID.randomUUID();

        Finding finding = EvidenceChecks.reused(copy, original);

        assertThat(finding.ruleCode()).isEqualTo("PHOTO_REUSED");
        assertThat(finding.severity()).isEqualTo(Severity.RED);
        assertThat(finding.expectedValue()).isEqualTo(original.toString());
        assertThat(finding.actualValue()).isEqualTo(copy.toString());
    }

    @Test
    @DisplayName("Sınıra eşit süre uyuşmazlık değildir; aşınca sınır ve geçen tam saat yazılır")
    void shouldFlagLongTransit() {
        TimeLimit limit = TimeLimit.REPORT_DEFAULT;

        assertThat(TimeChecks.inTransit(
                        DISPATCHED,
                        DISPATCHED.plus(Duration.ofHours(TimeLimit.REPORT_DEFAULT.inTransitMaxHours())),
                        limit))
                .isEmpty();
        Finding finding = TimeChecks.inTransit(DISPATCHED, DISPATCHED.plus(THIRTY_HOURS_AND_A_BIT), limit)
                .orElseThrow();
        assertThat(finding.type()).isEqualTo(DiscrepancyType.TIME);
        assertThat(finding.ruleCode()).isEqualTo("TIME_IN_TRANSIT_EXCEEDED");
        assertThat(finding.expectedValue()).isEqualTo("24");
        assertThat(finding.actualValue()).isEqualTo("30");
    }

    @Test
    @DisplayName("Süre sınırı 1–720 saat arasıdır")
    void shouldValidateTimeLimit() {
        assertThat(new TimeLimit(TimeLimit.MAX_HOURS).inTransitMaxHours()).isEqualTo(TimeLimit.MAX_HOURS);
        assertThatThrownBy(() -> new TimeLimit(0)).isInstanceOf(InvalidSettingException.class);
        assertThatThrownBy(() -> new TimeLimit(TimeLimit.MAX_HOURS + 1)).isInstanceOf(InvalidSettingException.class);
    }
}
