package com.yarnshipment.shipment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.shipment.domain.arrival.ShipmentArrived;
import com.yarnshipment.shipment.domain.cancellation.ShipmentCancelled;
import com.yarnshipment.shipment.domain.check.ShipmentCompleted;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Sevkiyatın yaşam döngüsü olayları, discrepancy-service'in okuduğu event-contracts kayıtlarına kayıpsız okunur. */
class ShipmentLifecycleContractTest {

    private static final Instant AT = Instant.parse("2026-10-22T09:00:00Z");
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID PARTY = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final int BAGS = 4;
    /** Sözleşmede olmayan alan yayınlanırsa test kırılır; sözleşme önce güncellenir. */
    private static final JsonMapper STRICT = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    @DisplayName("Oluşturma, yola çıkış, varış, tamamlanma ve iptal olayları sözleşmeyle aynı tip ve alanları taşır")
    void shouldMatchEventContracts() {
        ShipmentCreated created = new ShipmentCreated(SHIPMENT, "SV-1", SITE, PARTY, AT);
        ShipmentDispatched dispatched = new ShipmentDispatched(SHIPMENT, SITE, PARTY, BAGS, AT);
        ShipmentArrived arrived = new ShipmentArrived(SHIPMENT, SITE, PARTY, true, AT);
        ShipmentCompleted completed = new ShipmentCompleted(SHIPMENT, SITE, AT);
        ShipmentCancelled cancelled = new ShipmentCancelled(SHIPMENT, SITE, AT);

        assertThat(roundTrip(created, com.yarnshipment.events.shipment.ShipmentCreated.class))
                .isEqualTo(new com.yarnshipment.events.shipment.ShipmentCreated(SHIPMENT, "SV-1", SITE, PARTY, AT));
        assertThat(roundTrip(dispatched, com.yarnshipment.events.shipment.ShipmentDispatched.class)
                        .dispatchedAt())
                .isEqualTo(AT);
        assertThat(roundTrip(arrived, com.yarnshipment.events.shipment.ShipmentArrived.class)
                        .arrivedAt())
                .isEqualTo(AT);
        assertThat(roundTrip(completed, com.yarnshipment.events.shipment.ShipmentCompleted.class)
                        .completedAt())
                .isEqualTo(AT);
        assertThat(roundTrip(cancelled, com.yarnshipment.events.shipment.ShipmentCancelled.class)
                        .cancelledAt())
                .isEqualTo(AT);
        assertThat(com.yarnshipment.events.shipment.ShipmentCreated.TYPE).isEqualTo(ShipmentCreated.TYPE);
        assertThat(com.yarnshipment.events.shipment.ShipmentDispatched.TYPE).isEqualTo(ShipmentDispatched.TYPE);
        assertThat(com.yarnshipment.events.shipment.ShipmentArrived.TYPE).isEqualTo(ShipmentArrived.TYPE);
        assertThat(com.yarnshipment.events.shipment.ShipmentCompleted.TYPE).isEqualTo(ShipmentCompleted.TYPE);
        assertThat(com.yarnshipment.events.shipment.ShipmentCancelled.TYPE).isEqualTo(ShipmentCancelled.TYPE);
    }

    private static <T> T roundTrip(Object event, Class<T> contract) {
        return STRICT.readValue(STRICT.writeValueAsString(event), contract);
    }
}
