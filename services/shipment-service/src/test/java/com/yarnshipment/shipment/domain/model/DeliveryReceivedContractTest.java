package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.TestDeliveries;
import com.yarnshipment.shipment.TestShipments;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Yayınlanan teslim olayı, tüketicilerin kullandığı event-contracts kaydına kayıpsız okunur. */
class DeliveryReceivedContractTest {

    private static final Instant NOW = Instant.parse("2026-10-22T09:00:00Z");
    /** Sözleşmede olmayan alan yayınlanırsa test kırılır; sözleşme önce güncellenir. */
    private static final JsonMapper STRICT = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    @DisplayName("Teslim olayı sözleşme kaydına okunur; bağlam, plaka, fasoncu, irsaliye ve mühür durumu taşınır")
    void shouldMatchEventContract() {
        // given
        DeliveryReceipt receipt = TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY)
                .receiveDelivery(TestShipments.SUBCONTRACTOR_ID, TestDeliveries.measurement(), NOW);
        DeliveryReceived event = DeliveryReceived.of(receipt, TestShipments.SHIPMENT_ID, List.of("UK-0001"));

        // when
        com.yarnshipment.events.shipment.DeliveryReceived read = STRICT.readValue(
                STRICT.writeValueAsString(event), com.yarnshipment.events.shipment.DeliveryReceived.class);

        // then
        assertThat(read.shipmentId()).isEqualTo(event.shipmentId());
        assertThat(read.delivery().netKg())
                .isEqualByComparingTo(event.delivery().netKg());
        assertThat(read.deliverySeal().condition()).isEqualTo(SealCondition.INTACT.name());
        com.yarnshipment.events.shipment.DeliveryReceived.Context context = Objects.requireNonNull(read.context());
        assertThat(context.siteId()).isEqualTo(TestShipments.SITE_ID);
        assertThat(context.shipmentNo()).isEqualTo("SV-2026-000123");
        assertThat(context.plateNo()).isEqualTo("34ABC123");
        assertThat(context.subcontractorId()).isEqualTo(TestShipments.SUBCONTRACTOR_ID);
        assertThat(context.document().signedDocumentEvidenceId()).isNull();
        assertThat(com.yarnshipment.events.shipment.DeliveryReceived.TYPE).isEqualTo(DeliveryReceived.TYPE);
    }
}
