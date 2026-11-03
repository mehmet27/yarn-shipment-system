package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.UpdateDiscrepancySettingsCommand;
import com.yarnshipment.discrepancy.application.port.out.TimeLimitRulePort;
import com.yarnshipment.discrepancy.application.port.out.ToleranceRulePort;
import com.yarnshipment.discrepancy.domain.model.InvalidSettingException;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscrepancySettingsServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final int DEFAULT_HOURS = 24;
    private static final int NEW_HOURS = 36;

    private final ToleranceRulePort tolerance = mock(ToleranceRulePort.class);
    private final TimeLimitRulePort timeLimits = mock(TimeLimitRulePort.class);
    private final DiscrepancySettingsService service =
            new DiscrepancySettingsService(tolerance, timeLimits, Clock.fixed(NOW, ZoneOffset.UTC));
    private final Actor admin = TestTokens.mfaActor("admin");

    @Test
    @DisplayName("Kural tanımlı değilse rapordaki varsayılanlar döner")
    void shouldFallBackToReportDefaults() {
        givenCurrent(Optional.empty(), Optional.empty());

        var settings = service.getSettings();

        assertThat(settings.tolerance()).isEqualTo(ToleranceBands.REPORT_DEFAULT);
        assertThat(settings.timeLimit()).isEqualTo(TimeLimit.REPORT_DEFAULT);
    }

    @Test
    @DisplayName("Yalnızca değişen kural yazılır; 0.5 ile 0.50 aynı banttır")
    void shouldReplaceOnlyChangedRule() {
        givenCurrent(Optional.of(ToleranceBands.REPORT_DEFAULT), Optional.of(TimeLimit.REPORT_DEFAULT));

        service.updateSettings(
                new UpdateDiscrepancySettingsCommand(admin, new BigDecimal("0.5"), new BigDecimal("1.5"), NEW_HOURS));

        verify(tolerance, never()).replaceGeneralRule(any(), any(), any());
        verify(timeLimits).replaceTimeLimit(new TimeLimit(NEW_HOURS), admin.userId(), NOW);
    }

    @Test
    @DisplayName("Yeni bantlar Admin'in kimliği ve şimdiki anla yazılır")
    void shouldReplaceTolerance() {
        givenCurrent(Optional.of(ToleranceBands.REPORT_DEFAULT), Optional.of(TimeLimit.REPORT_DEFAULT));

        service.updateSettings(new UpdateDiscrepancySettingsCommand(
                admin, new BigDecimal("1.00"), new BigDecimal("2.00"), DEFAULT_HOURS));

        verify(tolerance)
                .replaceGeneralRule(
                        new ToleranceBands(new BigDecimal("1.00"), new BigDecimal("2.00")), admin.userId(), NOW);
        verify(timeLimits, never()).replaceTimeLimit(any(), any(), any());
    }

    @Test
    @DisplayName("Kural dışı değer hiçbir şey yazmadan reddedilir")
    void shouldRejectInvalidValuesBeforeWriting() {
        assertThatThrownBy(() -> service.updateSettings(
                        new UpdateDiscrepancySettingsCommand(admin, BigDecimal.TWO, BigDecimal.ONE, DEFAULT_HOURS)))
                .isInstanceOf(InvalidSettingException.class);
        assertThatThrownBy(() -> service.updateSettings(
                        new UpdateDiscrepancySettingsCommand(admin, BigDecimal.ONE, BigDecimal.TWO, 0)))
                .isInstanceOf(InvalidSettingException.class);

        verify(tolerance, never()).replaceGeneralRule(any(), any(), any());
        verify(timeLimits, never()).replaceTimeLimit(any(), any(), any());
    }

    private void givenCurrent(Optional<ToleranceBands> bands, Optional<TimeLimit> limit) {
        when(tolerance.findGeneralRule(NOW)).thenReturn(bands);
        when(timeLimits.findTimeLimit(NOW)).thenReturn(limit);
        when(tolerance.findGeneralRuleHistory(anyInt())).thenReturn(List.of());
        when(timeLimits.findTimeLimitHistory(anyInt())).thenReturn(List.of());
    }
}
