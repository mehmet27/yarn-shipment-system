package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.application.port.in.OpenReusedEvidenceCommand;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyOpeningPort;
import com.yarnshipment.discrepancy.application.port.out.OpenedDiscrepancy;
import com.yarnshipment.discrepancy.application.port.out.ProcessedEventPort;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import com.yarnshipment.discrepancy.application.port.out.TimeLimitRulePort;
import com.yarnshipment.discrepancy.domain.rule.TimeLimit;
import com.yarnshipment.discrepancy.domain.rule.WatchedShipment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Tekrar kullanılan fotoğraf ve yolda süre aşımı uyuşmazlıklarının açılması. */
class RuleServicesTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final int LIMIT_HOURS = 12;
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e3");
    private static final WatchedShipment WATCHED =
            new WatchedShipment(SHIPMENT, "SV-3", SITE, null, NOW.minus(Duration.ofHours(30)));

    private final ProcessedEventPort processed = mock(ProcessedEventPort.class);
    private final ShipmentWatchPort watch = mock(ShipmentWatchPort.class);
    private final DiscrepancyOpeningPort opening = mock(DiscrepancyOpeningPort.class);
    private final TimeLimitRulePort timeLimits = mock(TimeLimitRulePort.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final OpenReusedEvidenceService reused = new OpenReusedEvidenceService(processed, watch, opening, clock);
    private final CheckTransitTimesService transit = new CheckTransitTimesService(watch, timeLimits, opening, clock);
    private final OpenReusedEvidenceCommand reuse =
            new OpenReusedEvidenceCommand(EVENT, "reuse", UUID.randomUUID(), SHIPMENT, UUID.randomUUID());

    @Test
    @DisplayName("Tekrar kullanılan fotoğraf izlenen sevkiyatta sistem hesabıyla, beş iş günü vadeyle açılır")
    void shouldOpenReusedPhoto() {
        when(processed.markProcessed(EVENT, "reuse")).thenReturn(true);
        when(watch.findShipment(SHIPMENT)).thenReturn(Optional.of(WATCHED));

        assertThat(reused.openReusedEvidence(reuse)).isTrue();

        OpenedDiscrepancy opened = captured();
        assertThat(opened.shipment()).isEqualTo(WATCHED);
        assertThat(opened.finding().ruleCode()).isEqualTo("PHOTO_REUSED");
        assertThat(opened.actorId()).isEqualTo(SystemDefaults.SYSTEM_ACTOR);
        assertThat(opened.dueAt()).isEqualTo(Instant.parse("2026-10-30T09:00:00Z"));
    }

    @Test
    @DisplayName("Tekrar gelen olay ya da izlenmeyen sevkiyat uyuşmazlık açmaz")
    void shouldSkipRepeatedOrUnwatched() {
        when(processed.markProcessed(EVENT, "reuse")).thenReturn(false, true);
        when(watch.findShipment(SHIPMENT)).thenReturn(Optional.empty());

        assertThat(reused.openReusedEvidence(reuse)).isFalse();
        assertThat(reused.openReusedEvidence(reuse)).isFalse();

        verify(opening, never()).openDiscrepancy(any());
    }

    @Test
    @DisplayName("Ayarlı sınırdan önce yola çıkmış sevkiyat taranır; işaretlenebilen sevkiyatta tek uyuşmazlık açılır")
    void shouldOpenTransitDiscrepancyOnce() {
        WatchedShipment other = new WatchedShipment(UUID.randomUUID(), "SV-4", SITE, null, WATCHED.dispatchedAt());
        when(timeLimits.findTimeLimit(NOW)).thenReturn(Optional.of(new TimeLimit(LIMIT_HOURS)));
        when(watch.findInTransitSince(NOW.minus(Duration.ofHours(LIMIT_HOURS)), CheckTransitTimesService.BATCH_SIZE))
                .thenReturn(List.of(WATCHED, other));
        when(watch.markTransitAlerted(SHIPMENT, NOW)).thenReturn(true);
        when(watch.markTransitAlerted(other.shipmentId(), NOW)).thenReturn(false);

        assertThat(transit.checkTransitTimes()).isEqualTo(1);

        OpenedDiscrepancy opened = captured();
        assertThat(opened.finding().ruleCode()).isEqualTo("TIME_IN_TRANSIT_EXCEEDED");
        assertThat(opened.finding().expectedValue()).isEqualTo("12");
        assertThat(opened.finding().actualValue()).isEqualTo("30");
    }

    @Test
    @DisplayName("Sınır tanımlı değilse 24 saat kullanılır")
    void shouldDefaultTo24Hours() {
        when(timeLimits.findTimeLimit(NOW)).thenReturn(Optional.empty());

        assertThat(transit.checkTransitTimes()).isZero();

        verify(watch)
                .findInTransitSince(
                        eq(NOW.minus(Duration.ofHours(TimeLimit.REPORT_DEFAULT.inTransitMaxHours()))),
                        eq(CheckTransitTimesService.BATCH_SIZE));
    }

    private OpenedDiscrepancy captured() {
        ArgumentCaptor<OpenedDiscrepancy> opened = ArgumentCaptor.forClass(OpenedDiscrepancy.class);
        verify(opening).openDiscrepancy(opened.capture());
        return opened.getValue();
    }
}
