package com.yarnshipment.audit.adapter.out.persistence;

import com.yarnshipment.audit.application.port.in.AppendAuditEntryCommand;
import java.time.Instant;
import java.util.UUID;

/** Testlerde zincire yazılan örnek kayıtlar. */
final class AuditEntries {

    static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000b1");
    static final UUID OTHER_SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000b2");
    static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    static final String LOADED = "com.yarnshipment.shipment.shipment-loaded.v1";
    static final String DELIVERED = "com.yarnshipment.shipment.delivery-received.v1";
    static final String PAYLOAD = "{\"status\":\"LOADED\"}";
    static final String SYSTEM_CLIENT = "discrepancy-service";

    private AuditEntries() {}

    /** Depo kullanıcısının sevkiyat üzerindeki işlemi. */
    static AppendAuditEntryCommand byUser(UUID eventId, Instant occurredAt, String action, UUID entityId) {
        return new AppendAuditEntryCommand(
                eventId,
                occurredAt,
                "shipment-service",
                new AppendAuditEntryCommand.Actor(ACTOR, "WAREHOUSE", null),
                action,
                "Shipment",
                entityId,
                PAYLOAD,
                "corr-1");
    }

    /** Kullanıcısız, servis hesabıyla yapılan işlem. */
    static AppendAuditEntryCommand bySystem(Instant occurredAt, String action, UUID entityId) {
        return new AppendAuditEntryCommand(
                UUID.randomUUID(),
                occurredAt,
                "shipment-service",
                new AppendAuditEntryCommand.Actor(null, null, SYSTEM_CLIENT),
                action,
                "Shipment",
                entityId,
                PAYLOAD,
                "corr-1");
    }
}
