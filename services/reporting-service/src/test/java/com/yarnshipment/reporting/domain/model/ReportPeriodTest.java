package com.yarnshipment.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Rapor döneminin varsayılanları ve sınırları. */
class ReportPeriodTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-03");

    @Test
    @DisplayName("Uçlar boşsa son 30 gün alınır")
    void shouldDefaultToLastThirtyDays() {
        ReportPeriod period = ReportPeriod.of(null, null, TODAY);

        assertThat(period.to()).isEqualTo(TODAY);
        assertThat(period.from()).isEqualTo(LocalDate.parse("2026-10-05"));
    }

    @Test
    @DisplayName("Yalnızca başlangıç verilirse bitiş bugündür")
    void shouldDefaultEndToToday() {
        ReportPeriod period = ReportPeriod.of(LocalDate.parse("2026-11-01"), null, TODAY);

        assertThat(period.to()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("Dönem sınırları Istanbul saatiyle hesaplanır ve bitiş günü dahildir")
    void shouldCoverWholeLastDayInIstanbulTime() {
        ReportPeriod period = new ReportPeriod(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-02"));

        assertThat(period.start()).isEqualTo(Instant.parse("2026-10-31T21:00:00Z"));
        assertThat(period.endExclusive()).isEqualTo(Instant.parse("2026-11-02T21:00:00Z"));
    }

    @Test
    @DisplayName("Başlangıç bitişten sonra olamaz")
    void shouldRejectReversedPeriod() {
        assertThatThrownBy(() -> new ReportPeriod(TODAY, TODAY.minusDays(1)))
                .isInstanceOf(InvalidReportPeriodException.class);
    }

    @Test
    @DisplayName("Dönem 366 günden uzun olamaz")
    void shouldRejectTooLongPeriod() {
        assertThatThrownBy(() -> new ReportPeriod(TODAY.minusDays(ReportPeriod.MAX_DAYS + 1L), TODAY))
                .isInstanceOf(InvalidReportPeriodException.class);
        assertThat(new ReportPeriod(TODAY.minusDays(ReportPeriod.MAX_DAYS), TODAY).to())
                .isEqualTo(TODAY);
    }
}
