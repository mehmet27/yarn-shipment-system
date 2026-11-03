package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Bir tarafın kantar tartımı. Net kilo brüt ile daradan hesaplanır, ayrıca saklanmaz.
 *
 * @param grossKg brüt kilo
 * @param tareKg dara
 * @param bagCount tartılan çuval sayısı
 * @param weighedAt tartım zamanı
 * @param tareBasis kayıt anındaki standart dara; tanımsızsa boş
 */
public record Weighing(
        BigDecimal grossKg, BigDecimal tareKg, int bagCount, Instant weighedAt, @Nullable TareBasis tareBasis) {

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

    public Weighing(BigDecimal grossKg, BigDecimal tareKg, int bagCount, Instant weighedAt) {
        this(grossKg, tareKg, bagCount, weighedAt, null);
    }

    public @Nullable BigDecimal expectedTareKg() {
        return tareBasis == null ? null : tareBasis.expectedKg();
    }

    public @Nullable BigDecimal tareTolerancePct() {
        return tareBasis == null ? null : tareBasis.tolerancePct();
    }

    /** Standarda göre dara sapması (%); standart tanımsızsa boş. */
    public @Nullable BigDecimal tareDeviationPct() {
        return tareBasis == null ? null : tareBasis.deviationPct(tareKg);
    }

    /** Dara izin verilen sapmayı aşıyor mu. */
    public boolean tareAlert() {
        return tareBasis != null && tareBasis.exceeded(tareKg);
    }

    public BigDecimal netKg() {
        return grossKg.subtract(tareKg);
    }
}
