package com.yarnshipment.reporting.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.outbox.IncomingEvent;
import com.yarnshipment.reporting.domain.model.ReadModelChange;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sevkiyat olaylarının okuma modeli değişikliğine çevrilmesi. */
class ShipmentChangeMapperTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a1");

    @Test
    @DisplayName("Çıkış olayı net kiloyu ve zamanı taşır; bilinmeyen alanlar yok sayılır")
    void shouldMapDispatched() {
        String json = "{\"shipmentId\":\"" + SHIPMENT + "\",\"siteId\":\"" + UUID.randomUUID()
                + "\",\"subcontractorId\":\"" + UUID.randomUUID() + "\",\"bagCount\":3,\"netKg\":1982.500,"
                + "\"dispatchedAt\":\"2026-11-03T08:00:00Z\","
                + "\"extra\":1}";

        ReadModelChange change =
                ShipmentChangeMapper.toChange(event("com.yarnshipment.shipment.shipment-dispatched.v2", json));

        assertThat(change)
                .isEqualTo(new ReadModelChange.ShipmentDispatched(
                        SHIPMENT, new BigDecimal("1982.500"), Instant.parse("2026-11-03T08:00:00Z")));
    }

    @Test
    @DisplayName("Kullanılmayan olay türü için değişiklik üretilmez")
    void shouldIgnoreUnknownType() {
        assertThat(ShipmentChangeMapper.toChange(event("com.yarnshipment.shipment.other.v1", "{}")))
                .isNull();
    }

    private static IncomingEvent event(String type, String data) {
        return new IncomingEvent(UUID.randomUUID(), type, SHIPMENT, Instant.parse("2026-11-03T08:00:00Z"), null, data);
    }
}
