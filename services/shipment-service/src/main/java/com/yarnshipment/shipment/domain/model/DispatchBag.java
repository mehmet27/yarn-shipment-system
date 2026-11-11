package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Çıkışta sevkiyata girilmiş çuval: depo çuvalları sıra numarası ve kiloyla yazar.
 *
 * @param seqNo sevkiyat içindeki sıra (1, 2, 3...)
 * @param weightKg çuvalın kilosu
 * @param assignedAt girildiği zaman
 */
public record DispatchBag(int seqNo, BigDecimal weightKg, Instant assignedAt) {

    public DispatchBag {
        Objects.requireNonNull(weightKg, "weightKg");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
