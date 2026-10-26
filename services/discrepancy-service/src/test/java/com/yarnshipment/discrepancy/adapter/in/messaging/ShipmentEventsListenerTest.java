package com.yarnshipment.discrepancy.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.OutboxEvent;
import com.yarnshipment.discrepancy.TestComparisons;
import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryCommand;
import com.yarnshipment.discrepancy.application.port.in.CompareDeliveryUseCase;
import com.yarnshipment.discrepancy.domain.model.ComparisonInput;
import com.yarnshipment.discrepancy.domain.model.SealCondition;
import com.yarnshipment.events.shipment.DeliveryReceived;
import com.yarnshipment.events.shipment.ShipmentEvents;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShipmentEventsListenerTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final String ACTOR = "0192f0a0-0000-7000-8000-00000000d001";
    private static final Instant AT = Instant.parse("2026-10-23T09:00:00Z");
    /** Sözleşmede olmayan {@code futureField} yok sayılır (EVT-06). */
    private static final String DELIVERY = """
            {"shipmentId":"%s","deliveryReceiptId":"0192f0a0-0000-7000-8000-0000000000f1",
             "submittedAt":"2026-10-23T09:00:00Z",
             "delivery":{"grossKg":2100.000,"tareKg":100.000,"netKg":2000.000,"bagCount":2},
             "dispatch":{"grossKg":2100.000,"tareKg":100.000,"netKg":2000.000,"bagCount":2},
             "expectedUniqueKeys":["UK-0001","UK-0002"],"scannedUniqueKeys":["UK-0002","UK-0001"],
             "dispatchSeal":{"sealNo":"M-0001","condition":null},
             "deliverySeal":{"sealNo":"M-0001","condition":"intact"},
             "context":{"siteId":"%s","shipmentNo":"SV-3",
                        "document":{"shipmentNo":"SV-3","date":"2026-10-23","netKg":2000.000,"bagCount":2,
                                    "signedDocumentEvidenceId":null}},
             "futureField":true}""".formatted(TestComparisons.SHIPMENT, TestComparisons.SITE);

    private final CompareDeliveryUseCase useCase = mock(CompareDeliveryUseCase.class);
    private final ShipmentEventsListener listener = new ShipmentEventsListener(useCase);

    @Test
    @DisplayName("Teslim girişi olayı karşılaştırma komutuna çevrilir; olay kimliği ve yapan kişi taşınır")
    void shouldCompareDeliveryReceived() {
        listener.onShipmentEvent(record(DeliveryReceived.TYPE, DELIVERY, Map.of("actorId", ACTOR)));

        ArgumentCaptor<CompareDeliveryCommand> command = ArgumentCaptor.forClass(CompareDeliveryCommand.class);
        verify(useCase).compareDelivery(command.capture());
        ComparisonInput input = command.getValue().input();
        assertThat(command.getValue().eventId()).isEqualTo(EVENT);
        assertThat(command.getValue().actorId()).isEqualTo(UUID.fromString(ACTOR));
        assertThat(input.siteId()).isEqualTo(TestComparisons.SITE);
        assertThat(input.dispatch().netKg()).isEqualByComparingTo("2000");
        assertThat(input.delivery().sealCondition()).isEqualTo(SealCondition.INTACT);
        assertThat(input.document().signedDocumentEvidenceId()).isNull();
        assertThat(input.document().bagCount()).isEqualTo(TestComparisons.BAGS);
    }

    @Test
    @DisplayName("Diğer sevkiyat olayları karşılaştırmaya gitmez")
    void shouldIgnoreOtherEvents() {
        listener.onShipmentEvent(record("com.yarnshipment.shipment.shipment-dispatched.v1", "{}", Map.of()));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Yapan kişi ya da bağlam taşımayan teslim olayı kalıcı hatadır")
    void shouldRejectIncompleteDelivery() {
        String withoutContext = DELIVERY.replaceFirst("(?s)\"context\":\\{.*?}},", "");

        assertThatThrownBy(() -> listener.onShipmentEvent(record(DeliveryReceived.TYPE, DELIVERY, Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.onShipmentEvent(
                        record(DeliveryReceived.TYPE, withoutContext, Map.of("actorId", ACTOR))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(useCase);
    }

    private static ConsumerRecord<String, String> record(String type, String payload, Map<String, String> headers) {
        OutboxEvent event = new OutboxEvent(EVENT, AT, TestComparisons.SHIPMENT, type, payload, headers, AT);
        ProducerRecord<String, String> produced =
                CloudEventHeaders.toRecord(ShipmentEvents.TOPIC, "/yarn-shipment/shipment-service", event);
        ConsumerRecord<String, String> consumed =
                new ConsumerRecord<>(ShipmentEvents.TOPIC, 0, 0L, produced.key(), produced.value());
        for (Header header : produced.headers()) {
            consumed.headers().add(header);
        }
        return consumed;
    }
}
