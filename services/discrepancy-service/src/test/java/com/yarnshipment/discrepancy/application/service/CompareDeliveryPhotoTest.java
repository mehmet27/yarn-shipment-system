package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryCommand;
import com.yarnshipment.discrepancy.application.port.out.ComparisonPort;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.application.port.out.ProcessedEventPort;
import com.yarnshipment.discrepancy.application.port.out.ShipmentEvidencePort;
import com.yarnshipment.discrepancy.application.port.out.ShipmentWatchPort;
import com.yarnshipment.discrepancy.application.port.out.TicketReadingPort;
import com.yarnshipment.discrepancy.application.port.out.ToleranceRulePort;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Teslim karşılaştırmasında çıkışın zorunlu fotoğrafları. */
class CompareDeliveryPhotoTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");

    private final ProcessedEventPort processed = mock(ProcessedEventPort.class);
    private final ComparisonPort comparisons = mock(ComparisonPort.class);
    private final TicketReadingPort tickets = mock(TicketReadingPort.class);
    private final ShipmentWatchPort watch = mock(ShipmentWatchPort.class);
    private final ShipmentEvidencePort evidence = mock(ShipmentEvidencePort.class);
    private final CompareDeliveryService service = new CompareDeliveryService(
            processed,
            mock(ToleranceRulePort.class),
            comparisons,
            tickets,
            watch,
            evidence,
            Clock.fixed(NOW, ZoneOffset.UTC));

    CompareDeliveryPhotoTest() {
        when(processed.markProcessed(any(), any())).thenReturn(true);
        when(tickets.findTickets(any())).thenReturn(ShipmentTickets.NONE);
    }

    @Test
    @DisplayName("İzlenen sevkiyatta yüklenmemiş her zorunlu çıkış fotoğrafı için PHOTO_MISSING açılır")
    void shouldAddMissingPhotoFindingsForWatchedShipment() {
        when(watch.findShipment(TestComparisons.SHIPMENT)).thenReturn(Optional.of(TestComparisons.watched()));
        when(evidence.findEvidenceTypes(TestComparisons.SHIPMENT)).thenReturn(Set.of("DISPATCH_SCALE", "SEAL"));

        service.compareDelivery(command());

        assertThat(saved().result().findings())
                .extracting(Finding::ruleCode, Finding::expectedValue)
                .containsExactly(tuple("PHOTO_MISSING", "BAG_OVERVIEW"), tuple("PHOTO_MISSING", "VEHICLE_PLATE"));
    }

    @Test
    @DisplayName("İzleme öncesi açılmış sevkiyatta yüklenen kanıtlar bilinmediği için fotoğraf aranmaz")
    void shouldSkipPhotoCheckForUnwatchedShipment() {
        when(watch.findShipment(TestComparisons.SHIPMENT)).thenReturn(Optional.empty());

        service.compareDelivery(command());

        assertThat(saved().result().clean()).isTrue();
    }

    private CompletedComparison saved() {
        ArgumentCaptor<CompletedComparison> saved = ArgumentCaptor.forClass(CompletedComparison.class);
        verify(comparisons).saveComparison(saved.capture());
        return saved.getValue();
    }

    private static CompareDeliveryCommand command() {
        return new CompareDeliveryCommand(EVENT, "delivery", ACTOR, TestComparisons.clean(), TestTickets.entries());
    }
}
