package com.yarnshipment.reporting.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.events.shipment.ShipmentEvents;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeCommand;
import com.yarnshipment.reporting.application.port.in.ApplyReadModelChangeUseCase;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** {@code shipment.events.v1} olaylarının okuma modeli komutuna çevrilmesi. */
class ShipmentEventsListenerTest {

    private static final String PREFIX = "com.yarnshipment.shipment.";
    private static final String IDS = "\"shipmentId\":\"" + EventRecords.SHIPMENT + "\",\"siteId\":\""
            + EventRecords.SITE + "\",\"driverId\":\"" + EventRecords.DRIVER + "\"";

    private final ApplyReadModelChangeUseCase useCase = mock(ApplyReadModelChangeUseCase.class);
    private final ShipmentEventsListener listener = new ShipmentEventsListener(useCase);

    @Test
    @DisplayName("Oluşturma olayı sevkiyat, lokasyon ve fasoncuyu taşır")
    void shouldMapCreated() {
        String json = "{" + IDS + ",\"shipmentNo\":\"SHP-1\",\"subcontractorId\":\"" + EventRecords.SUBCONTRACTOR
                + "\",\"createdAt\":\"" + EventRecords.AT + "\"}";

        ReadModelChange change = apply("shipment-created.v1", json);

        assertThat(change).isInstanceOf(ReadModelChange.ShipmentCreated.class);
        assertThat(((ReadModelChange.ShipmentCreated) change).subcontractorId()).isEqualTo(EventRecords.SUBCONTRACTOR);
    }

    @Test
    @DisplayName("Varış, tamamlanma ve iptal olayları ilgili değişikliğe çevrilir")
    void shouldMapStatusEvents() {
        assertThat(apply(
                        "shipment-arrived.v1",
                        "{" + IDS + ",\"sealIntact\":true,\"arrivedAt\":\"" + EventRecords.AT + "\"}"))
                .isInstanceOf(ReadModelChange.ShipmentArrived.class);
        assertThat(apply("shipment-completed.v1", "{" + IDS + ",\"completedAt\":\"" + EventRecords.AT + "\"}"))
                .isInstanceOf(ReadModelChange.ShipmentCompleted.class);
        assertThat(apply("shipment-cancelled.v1", "{" + IDS + ",\"cancelledAt\":\"" + EventRecords.AT + "\"}"))
                .isInstanceOf(ReadModelChange.ShipmentCancelled.class);
    }

    @Test
    @DisplayName("Teslim girişi olayı çıkış ve teslim netini taşır; çıkış tartısı yoksa boş kalır")
    void shouldMapDelivery() {
        String weighing = "{\"grossKg\":2100.000,\"tareKg\":100.000,\"netKg\":2000.000,\"bagCount\":2}";
        String seal = "{\"sealNo\":\"M-1\",\"condition\":null}";
        String json = "{\"shipmentId\":\"" + EventRecords.SHIPMENT + "\",\"deliveryReceiptId\":\""
                + java.util.UUID.randomUUID() + "\",\"submittedAt\":\"" + EventRecords.AT + "\",\"delivery\":"
                + weighing + ",\"dispatch\":null,\"expectedUniqueKeys\":[],\"scannedUniqueKeys\":[],"
                + "\"dispatchSeal\":" + seal + ",\"deliverySeal\":" + seal + "}";

        ReadModelChange change = apply("delivery-received.v1", json);

        ReadModelChange.DeliveryReceived delivery = (ReadModelChange.DeliveryReceived) change;
        assertThat(delivery.dispatchNetKg()).isNull();
        assertThat(delivery.deliveryNetKg()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("Kullanılmayan olay türü onaylanır ve okuma modeline gitmez")
    void shouldIgnoreUnusedEvent() {
        listener.onShipmentEvent(EventRecords.record(ShipmentEvents.TOPIC, PREFIX + "other.v1", "{}"));

        verifyNoInteractions(useCase);
    }

    private ReadModelChange apply(String type, String json) {
        listener.onShipmentEvent(EventRecords.record(ShipmentEvents.TOPIC, PREFIX + type, json));
        ArgumentCaptor<ApplyReadModelChangeCommand> command =
                ArgumentCaptor.forClass(ApplyReadModelChangeCommand.class);
        verify(useCase, atLeastOnce()).applyChange(command.capture());
        ApplyReadModelChangeCommand last = command.getValue();
        assertThat(last.eventType()).isEqualTo(PREFIX + type);
        return last.change();
    }
}
