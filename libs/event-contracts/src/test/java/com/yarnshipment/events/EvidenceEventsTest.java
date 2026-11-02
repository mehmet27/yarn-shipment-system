package com.yarnshipment.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.evidence.EvidenceInvalidated;
import com.yarnshipment.events.evidence.EvidenceReuseDetected;
import com.yarnshipment.events.evidence.EvidenceUploaded;
import com.yarnshipment.events.evidence.TicketRead;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EvidenceEventsTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final Instant AT = Instant.parse("2026-10-27T09:00:00Z");
    private static final String SHA = "ab".repeat(32);

    @Test
    @DisplayName("Kanıt olayları JSON'a yazılıp aynen okunur")
    void shouldRoundTripEvidenceEvents() {
        UUID evidence = UUID.randomUUID();
        EvidenceUploaded uploaded = new EvidenceUploaded(evidence, SHIPMENT, "DISPATCH_SCALE", SHA, AT);
        EvidenceReuseDetected reuse = new EvidenceReuseDetected(evidence, SHIPMENT, UUID.randomUUID(), SHIPMENT, AT);

        assertThat(JSON.readValue(JSON.writeValueAsString(uploaded), EvidenceUploaded.class))
                .isEqualTo(uploaded);
        assertThat(JSON.readValue(JSON.writeValueAsString(reuse), EvidenceReuseDetected.class))
                .isEqualTo(reuse);
        EvidenceInvalidated invalidated = new EvidenceInvalidated(evidence, SHIPMENT, "SEAL", "yanlış yükleme", AT);
        assertThat(JSON.readValue(JSON.writeValueAsString(invalidated), EvidenceInvalidated.class))
                .isEqualTo(invalidated);
    }

    @Test
    @DisplayName("Fiş okundu olayı kilolarla da kilosuz da JSON'a yazılıp aynen okunur")
    void shouldRoundTripTicketRead() {
        TicketRead read = new TicketRead(
                UUID.randomUUID(),
                SHIPMENT,
                "DISPATCH_SCALE",
                "READ",
                new TicketRead.Weights(new BigDecimal("12480"), new BigDecimal("8210"), new BigDecimal("4270")),
                new TicketRead.Fields("34 ABC 123", AT, "004512", "Merkez Kantar"),
                List.of(),
                AT);
        TicketRead unreadable = new TicketRead(
                UUID.randomUUID(),
                SHIPMENT,
                "DELIVERY_SCALE",
                "UNREADABLE",
                null,
                new TicketRead.Fields(null, null, null, null),
                List.of("WEIGHT_NOT_READ", "BLURRY"),
                AT);

        assertThat(JSON.readValue(JSON.writeValueAsString(read), TicketRead.class))
                .isEqualTo(read);
        assertThat(JSON.readValue(JSON.writeValueAsString(unreadable), TicketRead.class))
                .isEqualTo(unreadable);
    }
}
