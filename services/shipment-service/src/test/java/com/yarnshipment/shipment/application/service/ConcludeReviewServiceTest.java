package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.application.port.in.check.ConcludeReviewCommand;
import com.yarnshipment.shipment.domain.check.CheckConclusion;
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

class ConcludeReviewServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T09:00:00Z");
    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e3");
    private static final String TYPE = "com.yarnshipment.discrepancy.discrepancy-resolved.v1";

    private final InMemoryShipmentQueryPort shipments = new InMemoryShipmentQueryPort();
    private final Set<UUID> processed = new HashSet<>();
    private final List<CheckConclusion> recorded = new ArrayList<>();
    private final ConcludeReviewService service = new ConcludeReviewService(
            shipments, (eventId, eventType) -> processed.add(eventId), recorded::add, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("İncelemedeki sevkiyat tamamlanır ve geçiş IN_REVIEW'dan COMPLETED'a yazılır")
    void shouldCompleteShipmentInReview() {
        shipments.add(TestShipments.shipment(ShipmentStatus.IN_REVIEW));

        service.concludeReview(command(EVENT));

        assertThat(recorded).singleElement().satisfies(conclusion -> {
            assertThat(conclusion.fromStatus()).isEqualTo(ShipmentStatus.IN_REVIEW);
            assertThat(conclusion.toStatus()).isEqualTo(ShipmentStatus.COMPLETED);
            assertThat(conclusion.concludedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("Tekrar gelen olay ve incelemede olmayan sevkiyat durumu değiştirmez")
    void shouldIgnoreRepeatedOrNonReviewShipments() {
        shipments.add(TestShipments.shipment(ShipmentStatus.IN_REVIEW));
        service.concludeReview(command(EVENT));
        service.concludeReview(command(EVENT));
        InMemoryShipmentQueryPort cancelled = new InMemoryShipmentQueryPort();
        cancelled.add(TestShipments.shipment(ShipmentStatus.CANCELLED));
        ConcludeReviewService other = new ConcludeReviewService(
                cancelled, (eventId, eventType) -> true, recorded::add, Clock.fixed(NOW, ZoneOffset.UTC));

        other.concludeReview(command(UUID.randomUUID()));

        assertThat(recorded).hasSize(1);
    }

    @Test
    @DisplayName("Bilinmeyen sevkiyat hata verir; olay yeniden denenip DLT'ye gider")
    void shouldFailForUnknownShipment() {
        assertThatThrownBy(() -> service.concludeReview(command(EVENT))).isInstanceOf(ShipmentNotFoundException.class);
        assertThat(processed).isEmpty();
    }

    private static ConcludeReviewCommand command(UUID eventId) {
        return new ConcludeReviewCommand(eventId, TYPE, TestShipments.SHIPMENT_ID);
    }
}
