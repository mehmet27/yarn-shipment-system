package com.yarnshipment.shipment.application.port.in.claim;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Haklı bulunan kalite itirazının bakiye düzeltmesi.
 *
 * @param discrepancyId itirazın uyuşmazlığı; aynı karar bir kez uygulanır
 * @param shipmentId itiraz edilen sevkiyat
 * @param claimedKg haklı bulunan kilo, sıfırdan büyük
 * @param decidedAt karar zamanı
 * @param decidedBy kararı veren kullanıcı
 */
public record RecordClaimAdjustmentCommand(
        UUID discrepancyId, UUID shipmentId, BigDecimal claimedKg, Instant decidedAt, UUID decidedBy) {

    public RecordClaimAdjustmentCommand {
        Objects.requireNonNull(discrepancyId, "discrepancyId");
        Objects.requireNonNull(shipmentId, "shipmentId");
        Objects.requireNonNull(claimedKg, "claimedKg");
        Objects.requireNonNull(decidedAt, "decidedAt");
        Objects.requireNonNull(decidedBy, "decidedBy");
    }
}
