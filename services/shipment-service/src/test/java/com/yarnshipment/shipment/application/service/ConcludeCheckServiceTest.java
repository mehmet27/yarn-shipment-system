package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckCommand;
import com.yarnshipment.shipment.domain.check.CheckConclusion;
import com.yarnshipment.shipment.domain.check.ComparisonOutcome;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcludeCheckServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final String TYPE = "com.yarnshipment.discrepancy.comparison-completed.v1";

    private final InMemoryShipmentQueryPort shipments = new InMemoryShipmentQueryPort();
    private final Set<UUID> processed = new HashSet<>();
    private final List<CheckConclusion> recorded = new ArrayList<>();
    private final ConcludeCheckService service = new ConcludeCheckService(
            shipments, (eventId, eventType) -> processed.add(eventId), recorded::add, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Kontroldeki sevkiyat karşılaştırma sonucuna göre sonuçlandırılır")
    void shouldConcludeCheckingShipment() {
        shipments.add(TestShipments.shipment(ShipmentStatus.CHECKING));

        service.concludeCheck(command(EVENT, ComparisonOutcome.DISCREPANCY));

        assertThat(recorded).singleElement().satisfies(conclusion -> {
            assertThat(conclusion.toStatus()).isEqualTo(ShipmentStatus.IN_REVIEW);
            assertThat(conclusion.concludedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("Tekrar gelen olay ve artık kontrolde olmayan sevkiyat durumu değiştirmez")
    void shouldIgnoreRepeatedOrLateEvents() {
        shipments.add(TestShipments.shipment(ShipmentStatus.CHECKING));
        service.concludeCheck(command(EVENT, ComparisonOutcome.CLEAN));
        service.concludeCheck(command(EVENT, ComparisonOutcome.CLEAN));
        InMemoryShipmentQueryPort completed = new InMemoryShipmentQueryPort();
        completed.add(TestShipments.shipment(ShipmentStatus.COMPLETED));
        ConcludeCheckService late = new ConcludeCheckService(
                completed, (eventId, eventType) -> true, recorded::add, Clock.fixed(NOW, ZoneOffset.UTC));

        late.concludeCheck(command(UUID.randomUUID(), ComparisonOutcome.CLEAN));

        assertThat(recorded).hasSize(1);
    }

    @Test
    @DisplayName("Bilinmeyen sevkiyatın sonucu hata verir; olay yeniden denenip DLT'ye gider")
    void shouldFailForUnknownShipment() {
        assertThatThrownBy(() -> service.concludeCheck(command(EVENT, ComparisonOutcome.CLEAN)))
                .isInstanceOf(ShipmentNotFoundException.class);
        assertThat(processed).isEmpty();
    }

    private static ConcludeCheckCommand command(UUID eventId, ComparisonOutcome outcome) {
        return new ConcludeCheckCommand(eventId, TYPE, TestShipments.SHIPMENT_ID, outcome);
    }
}
