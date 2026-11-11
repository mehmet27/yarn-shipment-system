package com.yarnshipment.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.discrepancy.DiscrepancyOpened;
import com.yarnshipment.events.discrepancy.DiscrepancyResolved;
import java.math.BigDecimal;
import java.time.Instant;
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
    @DisplayName("Açılış olayı JSON'a yazılıp aynen okunur")
    void shouldRoundTripDiscrepancyOpened() {
        DiscrepancyOpened event = new DiscrepancyOpened(
                UUID.randomUUID(), "UY-1-1", SHIPMENT, SITE, "QUALITY", "RED", "QUALITY_CLAIM", AT);

        DiscrepancyOpened read = JSON.readValue(JSON.writeValueAsString(event), DiscrepancyOpened.class);

        assertThat(read).isEqualTo(event);
    }

    @Test
    @DisplayName("Çözüm olayı JSON'a yazılıp aynen okunur; açık uyuşmazlık kalmayınca hepsi çözülmüş sayılır")
    void shouldRoundTripDiscrepancyResolved() {
        DiscrepancyResolved event =
                new DiscrepancyResolved(UUID.randomUUID(), "UY-1-1", SHIPMENT, SITE, "TOLERANCE", AT, 0, null);

        DiscrepancyResolved read = JSON.readValue(JSON.writeValueAsString(event), DiscrepancyResolved.class);

        assertThat(read).isEqualTo(event);
        assertThat(read.allResolved()).isTrue();
    }

    @Test
    @DisplayName("Haklı itiraz kararı kiloyu taşır; kilosuz eski olay da okunur")
    void shouldCarryClaimedKgAndReadOlderEvents() {
        DiscrepancyResolved event = new DiscrepancyResolved(
                UUID.randomUUID(), "UY-1-2", SHIPMENT, SITE, "CLAIM_JUSTIFIED", AT, 0, new BigDecimal("100.000"));

        assertThat(JSON.readValue(JSON.writeValueAsString(event), DiscrepancyResolved.class))
                .isEqualTo(event);
        String legacy = JSON.writeValueAsString(event).replaceAll(",\"claimedKg\":[^,}]+", "");
        assertThat(JSON.readValue(legacy, DiscrepancyResolved.class).claimedKg())
                .isNull();
    }
}
