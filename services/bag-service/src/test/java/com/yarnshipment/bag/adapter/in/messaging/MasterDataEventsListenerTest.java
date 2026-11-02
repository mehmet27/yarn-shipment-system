package com.yarnshipment.bag.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.bag.application.port.in.RecordLotQualityLockCommand;
import com.yarnshipment.bag.application.port.in.RecordLotQualityLockUseCase;
import com.yarnshipment.common.outbox.CloudEventHeaders;
import com.yarnshipment.common.outbox.OutboxEvent;
import com.yarnshipment.events.masterdata.LotQualityLocked;
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

class MasterDataEventsListenerTest {

    private static final UUID EVENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000e1");
    private static final UUID LOT = UUID.fromString("0192f0a0-0000-7000-8000-000000000103");
    private static final Instant AT = Instant.parse("2026-10-30T10:00:00Z");

    /** Sözleşmede olmayan {@code futureField} yok sayılır (EVT-06). */
    private static final String LOCKED = """
            {"lotId":"%s","siteId":"0192f0a0-0000-7000-8000-000000000001","lotNo":"P-2026-0003",
             "productId":"0192f0a0-0000-7000-8000-000000000202","qualityGradeCode":"A",
             "lockedAt":"2026-10-30T10:00:00Z","futureField":1}""".formatted(LOT);

    private final RecordLotQualityLockUseCase useCase = mock(RecordLotQualityLockUseCase.class);
    private final MasterDataEventsListener listener = new MasterDataEventsListener(useCase);

    @Test
    @DisplayName("Parti kalite kilidi olayı kayıt komutuna çevrilir; olay kimliği idempotency için taşınır")
    void shouldRecordLotQualityLock() {
        listener.onShipmentTopicEvent(record(LotQualityLocked.TYPE, LOCKED));

        ArgumentCaptor<RecordLotQualityLockCommand> command =
                ArgumentCaptor.forClass(RecordLotQualityLockCommand.class);
        verify(useCase).recordLotQualityLock(command.capture());
        assertThat(command.getValue().eventId()).isEqualTo(EVENT);
        assertThat(command.getValue().lock().lotId()).isEqualTo(LOT);
        assertThat(command.getValue().lock().lotNo()).isEqualTo("P-2026-0003");
        assertThat(command.getValue().lock().qualityGradeCode()).isEqualTo("A");
        assertThat(command.getValue().lock().lockedAt()).isEqualTo(AT);
    }

    @Test
    @DisplayName("Aynı konudaki sevkiyat olayları atlanır")
    void shouldIgnoreShipmentEvents() {
        listener.onShipmentTopicEvent(record("com.yarnshipment.shipment.delivery-received.v1", "{}"));

        verifyNoInteractions(useCase);
    }

    private static ConsumerRecord<String, String> record(String type, String payload) {
        OutboxEvent event = new OutboxEvent(EVENT, AT, LOT, type, payload, Map.of(), AT);
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
