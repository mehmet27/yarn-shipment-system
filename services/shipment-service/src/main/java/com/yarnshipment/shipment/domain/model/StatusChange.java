package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sevkiyatın bir durum geçişi (DOM-14). İlk kayıtta önceki durum yoktur.
 *
 * @param from önceki durum
 * @param to yeni durum
 * @param changedAt geçiş zamanı
 * @param changedBy işlemi yapan kullanıcının kimliği
 * @param reason iptal ve düzeltme gerekçesi
 */
public record StatusChange(
        @Nullable ShipmentStatus from,
        ShipmentStatus to,
        Instant changedAt,
        UUID changedBy,
        @Nullable String reason) {

    public StatusChange {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(changedAt, "changedAt");
        Objects.requireNonNull(changedBy, "changedBy");
    }
}
