package com.yarnshipment.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.shipment.ShipmentCancelled;
import com.yarnshipment.events.shipment.ShipmentCompleted;
import com.yarnshipment.events.shipment.ShipmentCreated;
import com.yarnshipment.events.shipment.ShipmentDispatched;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ShipmentEventsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final UUID PARTY = UUID.fromString("0192f0a0-0000-7000-8000-000000000003");
    private static final Instant AT = Instant.parse("2026-10-23T09:00:00Z");
    private static final int BAGS = 2;

    @Test
    @DisplayName("Sevkiyat yaşam döngüsü olayları JSON'a yazılıp aynen okunur")
    void shouldRoundTripShipmentLifecycle() {
        List<Object> events = List.of(
                new ShipmentCreated(SHIPMENT, "SV-1", SITE, PARTY, AT),
                new ShipmentDispatched(SHIPMENT, SITE, PARTY, BAGS, new BigDecimal("1260.500"), AT),
                new ShipmentCompleted(SHIPMENT, SITE, AT),
                new ShipmentCancelled(SHIPMENT, SITE, AT));

        for (Object event : events) {
            assertThat(JSON.readValue(JSON.writeValueAsString(event), event.getClass()))
                    .isEqualTo(event);
        }
    }
}
