package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Olaydan okuma modeline yansıyan değişiklik. */
public sealed interface ReadModelChange {

    /** Sevkiyat taslağı oluştu. */
    record ShipmentCreated(UUID shipmentId, String shipmentNo, UUID siteId, UUID subcontractorId, Instant createdAt)
            implements ReadModelChange {}

    /** Şoför yükü teslim aldı. */
    record ShipmentDispatched(UUID shipmentId, BigDecimal netKg, Instant dispatchedAt) implements ReadModelChange {}

    /** Sevkiyat tamamlandı. */
    record ShipmentCompleted(UUID shipmentId, Instant completedAt) implements ReadModelChange {}

    /** Sevkiyat iptal edildi. */
    record ShipmentCancelled(UUID shipmentId) implements ReadModelChange {}

    /** Uyuşmazlık açıldı. */
    record DiscrepancyOpened(
            UUID discrepancyId,
            String discrepancyNo,
            UUID shipmentId,
            UUID siteId,
            String type,
            String severity,
            Instant dueAt,
            Instant openedAt)
            implements ReadModelChange {}

    /** Uyuşmazlık karara bağlandı. */
    record DiscrepancyResolved(UUID discrepancyId, UUID shipmentId, Instant resolvedAt, int remainingOpen)
            implements ReadModelChange {}
}
