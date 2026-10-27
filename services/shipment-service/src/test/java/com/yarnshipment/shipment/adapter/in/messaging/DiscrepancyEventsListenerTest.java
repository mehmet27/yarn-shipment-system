package com.yarnshipment.shipment.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.OutboxEvent;
import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import com.yarnshipment.events.discrepancy.DiscrepancyEvents;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckCommand;
import com.yarnshipment.shipment.application.port.in.check.ConcludeCheckUseCase;
import com.yarnshipment.shipment.application.port.in.check.ConcludeReviewCommand;
import com.yarnshipment.shipment.application.port.in.check.ConcludeReviewUseCase;
import com.yarnshipment.shipment.domain.check.ComparisonOutcome;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DiscrepancyEventsListenerTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e2");
    private static final Instant AT = Instant.parse("2026-10-23T09:00:00Z");
    /** Sözleşmede olmayan {@code futureField} yok sayılır (EVT-06). */
    private static final String COMPARISON = """
            {"shipmentId":"%s","siteId":"%s","comparedAt":"2026-10-23T09:00:00Z","outcome":"%s",
             "quantityBand":"GREEN","diffKg":0,"diffPct":0.00,"discrepancyCount":%d,"highestSeverity":%s,
             "futureField":1}""";

    private final ConcludeCheckUseCase useCase = mock(ConcludeCheckUseCase.class);
    private final ConcludeReviewUseCase reviewUseCase = mock(ConcludeReviewUseCase.class);
    private final DiscrepancyEventsListener listener = new DiscrepancyEventsListener(useCase, reviewUseCase);

    @Test
    @DisplayName("Karşılaştırma sonucu olayı komuta çevrilir; uyuşmazlık sonucu taşınır")
    void shouldConcludeCheckFromComparison() {
        String payload = COMPARISON.formatted(
                TestShipments.SHIPMENT_ID, TestShipments.SITE_ID, ComparisonCompleted.DISCREPANCY, 2, "\"RED\"");

        listener.onDiscrepancyEvent(record(ComparisonCompleted.TYPE, payload));

        ArgumentCaptor<ConcludeCheckCommand> command = ArgumentCaptor.forClass(ConcludeCheckCommand.class);
        verify(useCase).concludeCheck(command.capture());
        assertThat(command.getValue())
                .isEqualTo(new ConcludeCheckCommand(
                        EVENT, ComparisonCompleted.TYPE, TestShipments.SHIPMENT_ID, ComparisonOutcome.DISCREPANCY));
    }

    @Test
    @DisplayName("Temiz sonuç CLEAN olarak iletilir")
    void shouldPassCleanOutcome() {
        String payload = COMPARISON.formatted(
                TestShipments.SHIPMENT_ID, TestShipments.SITE_ID, ComparisonCompleted.CLEAN, 0, "null");

        listener.onDiscrepancyEvent(record(ComparisonCompleted.TYPE, payload));

        ArgumentCaptor<ConcludeCheckCommand> command = ArgumentCaptor.forClass(ConcludeCheckCommand.class);
        verify(useCase).concludeCheck(command.capture());
        assertThat(command.getValue().outcome()).isEqualTo(ComparisonOutcome.CLEAN);
    }

    @Test
    @DisplayName("Diğer uyuşmazlık olayları sevkiyat durumunu değiştirmez")
    void shouldIgnoreOtherEvents() {
        listener.onDiscrepancyEvent(record("com.yarnshipment.discrepancy.discrepancy-opened.v1", "{}"));

        verifyNoInteractions(useCase, reviewUseCase);
    }

    @Test
    @DisplayName("Son uyuşmazlık çözülünce inceleme biter; açık kalan varsa bitmez")
    void shouldConcludeReviewOnlyWhenAllResolved() {
        listener.onDiscrepancyEvent(record(DiscrepancyResolved.TYPE, resolved(1)));
        verifyNoInteractions(reviewUseCase);

        listener.onDiscrepancyEvent(record(DiscrepancyResolved.TYPE, resolved(0)));

        verify(reviewUseCase)
                .concludeReview(new ConcludeReviewCommand(EVENT, DiscrepancyResolved.TYPE, TestShipments.SHIPMENT_ID));
        verifyNoInteractions(useCase);
    }

    private static String resolved(int remainingOpen) {
        return "{\"discrepancyId\":\"" + UUID.randomUUID() + "\",\"discrepancyNo\":\"UY-1-1\",\"shipmentId\":\""
                + TestShipments.SHIPMENT_ID + "\",\"siteId\":\"" + TestShipments.SITE_ID
                + "\",\"resolution\":\"TOLERANCE\",\"resolvedAt\":\"2026-10-24T09:00:00Z\",\"remainingOpen\":"
                + remainingOpen + "}";
    }

    private static ConsumerRecord<String, String> record(String type, String payload) {
        OutboxEvent event = new OutboxEvent(EVENT, AT, TestShipments.SHIPMENT_ID, type, payload, Map.of(), AT);
        ProducerRecord<String, String> produced =
                CloudEventHeaders.toRecord(DiscrepancyEvents.TOPIC, "/yarn-shipment/discrepancy-service", event);
        ConsumerRecord<String, String> consumed =
                new ConsumerRecord<>(DiscrepancyEvents.TOPIC, 0, 0L, produced.key(), produced.value());
        for (Header header : produced.headers()) {
            consumed.headers().add(header);
        }
        return consumed;
    }
}
