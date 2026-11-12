package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Bir tarafın kantar tartımı. Net kilo brüt ile daradan hesaplanır, ayrıca saklanmaz.
 *
 * @param grossKg brüt kilo
 * @param tareKg dara
 * @param bagCount tartılan çuval sayısı
 * @param weighedAt tartım zamanı
 */
public record Weighing(BigDecimal grossKg, BigDecimal tareKg, int bagCount, Instant weighedAt) {

    public Weighing {
        Objects.requireNonNull(grossKg, "grossKg");
        Objects.requireNonNull(tareKg, "tareKg");
        Objects.requireNonNull(weighedAt, "weighedAt");
        if (tareKg.signum() < 0 || grossKg.compareTo(tareKg) < 0) {
            throw new IllegalArgumentException("Tare must be between zero and gross weight");
        }
        if (bagCount <= 0) {
            throw new IllegalArgumentException("Bag count must be positive");
        }
    }

    public BigDecimal netKg() {
        return grossKg.subtract(tareKg);
    }
}
