package com.yarnshipment.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.InvalidReportPeriodException;
import com.yarnshipment.reporting.domain.model.MonthlyFlow;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.YarnVsFabricRow;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Aylık eğilim: ay sınırları, sıralama ve "Giden iplik ↔ gelen kumaş" raporuyla tutarlılık. */
class MonthlyTrendServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    private static final ReportPeriod NOVEMBER =
            new ReportPeriod(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-30"));
    private static final YearMonth OCTOBER_MONTH = YearMonth.parse("2026-10");
    private static final int THREE_MONTHS = 3;
    private static final int TOO_MANY_MONTHS = 13;
    private static final ReportPeriod OCTOBER =
            new ReportPeriod(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

    private final BalanceFirePort port = mock(BalanceFirePort.class);
    private final ReportAccess access = new ReportAccess(new ScopeGuard(mock(AuditPublisher.class), CLOCK));
    private final MonthlyTrendService service = new MonthlyTrendService(port, access, CLOCK);

    @Test
    @DisplayName("Aylar eskiden yeniye sıralıdır; son ay bugünün ayıdır ve verisi olmayan ay sıfırdır")
    void shouldListCalendarMonthsOldestFirst() {
        when(port.findFireFiles(any(), any(), eq(null))).thenReturn(List.of());
        when(port.findFireFiles(eq(NOVEMBER), any(), eq(null))).thenReturn(List.of(file("1000", "900")));

        List<MonthlyFlow> trend = service.listMonthlyTrend(query(null), THREE_MONTHS);

        assertThat(trend)
                .extracting(MonthlyFlow::month)
                .containsExactly(YearMonth.parse("2026-09"), OCTOBER_MONTH, YearMonth.parse("2026-11"));
        assertThat(trend.get(0).sentKg()).isEqualByComparingTo("0");
        assertThat(trend.get(0).actualFireRate()).isEqualByComparingTo("0");
        assertThat(trend.get(2).shipmentCount()).isEqualTo(1);
        assertThat(trend.get(2).sentKg()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("Bitiş tarihi verilirse son ay o tarihin ayıdır")
    void shouldEndAtMonthOfGivenDate() {
        when(port.findFireFiles(any(), any(), eq(null))).thenReturn(List.of());

        List<MonthlyFlow> trend = service.listMonthlyTrend(query(LocalDate.parse("2026-10-05")), 1);

        assertThat(trend).extracting(MonthlyFlow::month).containsExactly(OCTOBER_MONTH);
    }

    @Test
    @DisplayName("Bir ayın sütunu, aynı ay için iplik-kumaş raporunun fasoncu satırlarının toplamına eşittir")
    void shouldMatchYarnVsFabricReportOfSameMonth() {
        List<FireFile> files = List.of(file("1000", "900"), file("2500.500", "2410.250"), file("340", "0"));
        when(port.findFireFiles(eq(OCTOBER), any(), eq(null))).thenReturn(files);

        MonthlyFlow october = service.listMonthlyTrend(query(OCTOBER.to()), 1).get(0);
        // İplik-kumaş raporu aynı dosyaları fasoncuya göre toplar (BalanceFireService.listYarnVsFabric).
        List<YarnVsFabricRow> rows = YarnVsFabricRow.aggregate(files);

        assertThat(october.sentKg()).isEqualByComparingTo(total(rows, YarnVsFabricRow::sentKg));
        assertThat(october.returnedYarnKg()).isEqualByComparingTo(total(rows, YarnVsFabricRow::returnedYarnKg));
        assertThat(october.fabricKg()).isEqualByComparingTo(total(rows, YarnVsFabricRow::fabricKg));
        assertThat(october.shortageKg()).isEqualByComparingTo(total(rows, YarnVsFabricRow::shortageKg));
        assertThat(october.shipmentCount()).isEqualTo(files.size());
    }

    @Test
    @DisplayName("Tek fasonculu ayda fire oranı iplik-kumaş raporundaki oranla aynıdır")
    void shouldUseSameFireRateFormula() {
        UUID sub = UUID.randomUUID();
        List<FireFile> files = List.of(file(sub, "1000", "900"), file(sub, "500", "470"));

        MonthlyFlow flow = MonthlyFlow.of(OCTOBER_MONTH, files);

        assertThat(flow.actualFireRate())
                .isEqualByComparingTo(YarnVsFabricRow.aggregate(files).get(0).actualFireRate());
    }

    @Test
    @DisplayName("Ay sayısı 1 ile 12 arasında değilse dönem hatası verir")
    void shouldRejectMonthCountOutOfRange() {
        assertThatThrownBy(() -> service.listMonthlyTrend(query(null), 0))
                .isInstanceOf(InvalidReportPeriodException.class);
        assertThatThrownBy(() -> service.listMonthlyTrend(query(null), TOO_MANY_MONTHS))
                .isInstanceOf(InvalidReportPeriodException.class);
    }

    @Test
    @DisplayName("Lokasyon kapsamı olmayan kullanıcı sıfırlı aylar alır, sorgu çalışmaz")
    void shouldReturnZeroMonthsWithoutScope() {
        ReportQuery query = new ReportQuery(TestTokens.actor(TestTokens.noSiteToken()), null, null, null);

        List<MonthlyFlow> trend = service.listMonthlyTrend(query, 2);

        assertThat(trend).hasSize(2).allMatch(flow -> flow.shipmentCount() == 0);
        verifyNoInteractions(port);
    }

    private static BigDecimal total(
            List<YarnVsFabricRow> rows, java.util.function.Function<YarnVsFabricRow, BigDecimal> getter) {
        return rows.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static ReportQuery query(@Nullable LocalDate to) {
        return new ReportQuery(TestTokens.actor("manager"), null, to, null);
    }

    private static FireFile file(String sent, String fabric) {
        return file(UUID.randomUUID(), sent, fabric);
    }

    private static FireFile file(UUID sub, String sent, String fabric) {
        return new FireFile(
                UUID.randomUUID(),
                "SHP",
                sub,
                "K",
                new BigDecimal(sent),
                BigDecimal.ZERO,
                new BigDecimal(fabric),
                new BigDecimal("0.04"),
                "TO_INVOICE",
                null,
                null,
                BigDecimal.ZERO);
    }
}
