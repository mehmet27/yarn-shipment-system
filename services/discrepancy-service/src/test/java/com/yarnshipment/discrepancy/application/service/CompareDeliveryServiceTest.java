package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryCommand;
import com.yarnshipment.discrepancy.application.port.out.ComparisonPort;
import com.yarnshipment.discrepancy.application.port.out.CompletedComparison;
import com.yarnshipment.discrepancy.application.port.out.ProcessedEventPort;
import com.yarnshipment.discrepancy.application.port.out.TicketReadingPort;
import com.yarnshipment.discrepancy.domain.model.Band;
import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.DeliveryFigures;
import com.yarnshipment.discrepancy.domain.model.Finding;
import com.yarnshipment.discrepancy.domain.model.SealCondition;
import com.yarnshipment.discrepancy.domain.model.TicketChecks;
import com.yarnshipment.discrepancy.domain.model.ToleranceBands;
import com.yarnshipment.discrepancy.domain.ticket.ShipmentTickets;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CompareDeliveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T09:00:00Z");
    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final String TYPE = "com.yarnshipment.shipment.delivery-received.v1";

    private final Set<UUID> processed = new HashSet<>();
    private final List<CompletedComparison> saved = new ArrayList<>();
    private @Nullable ToleranceBands rule;

    private final ProcessedEventPort processedEvents = (eventId, eventType) -> processed.add(eventId);
    private final ComparisonPort comparisons = new ComparisonPort() {
        @Override
        public boolean hasComparison(UUID shipmentId) {
            return saved.stream().anyMatch(c -> c.input().shipmentId().equals(shipmentId));
        }

        @Override
        public void saveComparison(CompletedComparison comparison) {
            saved.add(comparison);
        }
    };
    private ShipmentTickets tickets = ShipmentTickets.NONE;
    private final TicketReadingPort ticketReadings = new TicketReadingPort() {
        @Override
        public void saveReading(TicketReading reading, UUID actorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ShipmentTickets findTickets(UUID shipmentId) {
            return tickets;
        }
    };
    private final CompareDeliveryService service = new CompareDeliveryService(
            processedEvents,
            at -> Optional.ofNullable(rule),
            comparisons,
            ticketReadings,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Teslim karşılaştırılır; son tarih beş iş günü sonrasıdır ve olayı yapan kişi kaydedilir")
    void shouldSaveComparisonWithDueDate() {
        service.compareDelivery(command(EVENT, TestComparisons.clean()));

        CompletedComparison comparison = saved.getFirst();
        assertThat(comparison.result().clean()).isTrue();
        assertThat(comparison.comparedAt()).isEqualTo(NOW);
        assertThat(comparison.dueAt()).isEqualTo(Instant.parse("2026-10-30T09:00:00Z"));
        assertThat(comparison.actorId()).isEqualTo(ACTOR);
    }

    @Test
    @DisplayName("Tanımlı genel kural varsayılan bantların yerine kullanılır")
    void shouldUseConfiguredRule() {
        rule = new ToleranceBands(new BigDecimal("2.00"), new BigDecimal("3.00"));
        ComparisonInput input = TestComparisons.input(
                TestComparisons.dispatch(),
                new DeliveryFigures(
                        new BigDecimal("1970.000"), TestComparisons.BAGS, TestComparisons.SEAL, SealCondition.INTACT),
                TestComparisons.KEYS,
                TestComparisons.document());

        service.compareDelivery(command(EVENT, input));

        assertThat(saved.getFirst().result().quantityBand()).isEqualTo(Band.GREEN);
    }

    @Test
    @DisplayName("Aynı olay ya da aynı sevkiyatın ikinci teslim olayı tekrar karşılaştırılmaz")
    void shouldIgnoreRepeatedDelivery() {
        service.compareDelivery(command(EVENT, TestComparisons.clean()));

        service.compareDelivery(command(EVENT, TestComparisons.clean()));
        service.compareDelivery(command(UUID.randomUUID(), TestComparisons.clean()));

        assertThat(saved).hasSize(1);
        assertThat(processed).hasSize(2);
    }

    @Test
    @DisplayName("O ana kadar okunmuş kantar fişleri girişlerle karşılaştırılır; tutmayan fiş uyuşmazlık açar")
    void shouldAddTicketFindings() {
        tickets = TestTickets.both(TestTickets.weights(), TestTickets.weights("2050.000", "100.000"));

        service.compareDelivery(command(EVENT, TestComparisons.clean()));

        assertThat(saved.getFirst().result().findings())
                .extracting(Finding::ruleCode)
                .containsExactly(TicketChecks.DELIVERY_WEIGHT_MISMATCH, TicketChecks.QUANTITY_TOLERANCE);
        assertThat(saved.getFirst().result().quantityBand()).isEqualTo(Band.GREEN);
    }

    private static CompareDeliveryCommand command(UUID eventId, ComparisonInput input) {
        return new CompareDeliveryCommand(eventId, TYPE, ACTOR, input, TestTickets.entries());
    }
}
