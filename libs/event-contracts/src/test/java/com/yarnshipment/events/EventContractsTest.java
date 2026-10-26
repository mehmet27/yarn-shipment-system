package com.yarnshipment.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.discrepancy.ComparisonCompleted;
import com.yarnshipment.events.shipment.DeliveryReceived;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EventContractsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-00000000a001");
    private static final Instant AT = Instant.parse("2026-10-23T09:00:00Z");

    @Test
    @DisplayName("Karşılaştırma sonucu JSON'a yazılıp aynen okunur; temiz sonuç bayrağı sonuçtan türer")
    void shouldRoundTripComparisonCompleted() {
        ComparisonCompleted event = new ComparisonCompleted(
                SHIPMENT,
                SITE,
                AT,
                ComparisonCompleted.DISCREPANCY,
                "RED",
                new BigDecimal("40.000"),
                new BigDecimal("2.00"),
                1,
                "RED");

        ComparisonCompleted read = JSON.readValue(JSON.writeValueAsString(event), ComparisonCompleted.class);

        assertThat(read).isEqualTo(event);
        assertThat(read.clean()).isFalse();
    }

    @Test
    @DisplayName("Teslim olayının anahtar listeleri değiştirilemez kopyadır; bağlamsız eski olay da okunur")
    void shouldCopyKeyListsAndAcceptMissingContext() {
        List<String> keys = new ArrayList<>(List.of("UK-0001"));
        DeliveryReceived.Weighing weighing = new DeliveryReceived.Weighing(
                new BigDecimal("2100.000"), new BigDecimal("100.000"), new BigDecimal("2000.000"), 1);
        DeliveryReceived event = new DeliveryReceived(
                SHIPMENT,
                UUID.randomUUID(),
                AT,
                weighing,
                null,
                keys,
                keys,
                new DeliveryReceived.Seal("M-0001", null),
                new DeliveryReceived.Seal("M-0001", "INTACT"),
                null);
        keys.add("UK-0002");

        DeliveryReceived read = JSON.readValue(JSON.writeValueAsString(event), DeliveryReceived.class);

        assertThat(event.expectedUniqueKeys()).containsExactly("UK-0001");
        assertThat(read.context()).isNull();
        assertThat(read.delivery().netKg()).isEqualByComparingTo("2000");
    }
}
