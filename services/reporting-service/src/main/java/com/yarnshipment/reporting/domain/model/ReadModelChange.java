package com.yarnshipment.reporting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Olaydan okuma modeline yansıyan değişiklik. */
public sealed interface ReadModelChange {

    /** Sevkiyat taslağı oluştu. */
    record ShipmentCreated(UUID shipmentId, String shipmentNo, UUID siteId, UUID subcontractorId, Instant createdAt)
            implements ReadModelChange {}

    /** Şoför yükü teslim aldı. */
    record ShipmentDispatched(UUID shipmentId, UUID driverId, Instant dispatchedAt) implements ReadModelChange {}

    /** Şoför fasoncuya vardı. */
    record ShipmentArrived(UUID shipmentId, UUID driverId) implements ReadModelChange {}

    /** Sevkiyat tamamlandı. */
    record ShipmentCompleted(UUID shipmentId, Instant completedAt) implements ReadModelChange {}

    /** Sevkiyat iptal edildi. */
    record ShipmentCancelled(UUID shipmentId) implements ReadModelChange {}

    /** Fasoncu teslim girişini gönderdi. */
    record DeliveryReceived(UUID shipmentId, @Nullable BigDecimal dispatchNetKg, BigDecimal deliveryNetKg)
            implements ReadModelChange {}

    /** Teslim girişi karşılaştırıldı. */
    record ComparisonCompleted(
            UUID shipmentId,
            Instant comparedAt,
            String band,
            @Nullable BigDecimal diffKg,
            @Nullable BigDecimal diffPct,
            int discrepancyCount)
            implements ReadModelChange {}

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
