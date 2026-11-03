package com.yarnshipment.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.evidence.TicketUnverified;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TicketUnverifiedTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    @DisplayName("Fiş doğrulanamadı olayı JSON'a yazılıp aynen okunur")
    void shouldRoundTripTicketUnverified() {
        TicketUnverified unverified = new TicketUnverified(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DELIVERY_SCALE",
                "TICKET_DAMAGED",
                Instant.parse("2026-11-03T09:00:00Z"));

        assertThat(JSON.readValue(JSON.writeValueAsString(unverified), TicketUnverified.class))
                .isEqualTo(unverified);
    }
}
