package com.yarnshipment.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.events.evidence.EvidenceReuseDetected;
import com.yarnshipment.events.evidence.EvidenceUploaded;
import java.time.Instant;
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
    }
}
