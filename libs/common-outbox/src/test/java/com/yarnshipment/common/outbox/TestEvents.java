package com.yarnshipment.common.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Testlerde ortak outbox satırı. */
final class TestEvents {

    static final UUID SHIPMENT_ID = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");
    static final Instant AT = Instant.parse("2026-10-21T09:00:00Z");
    static final String TYPE = "com.yarnshipment.shipment.shipment-dispatched.v1";

    private TestEvents() {}

    static OutboxEvent event(UUID id) {
        return new OutboxEvent(id, AT, SHIPMENT_ID, TYPE, "{\"bagCount\":40}", Map.of("actorId", "user-1"), AT);
    }
}
