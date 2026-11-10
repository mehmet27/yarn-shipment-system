package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Depo çalışanının çıkışta girdiği değerler.
 *
 * @param grossKg brüt kilo
 * @param tareKg dara
 * @param bagCount tartılan çuval sayısı
 * @param scaleTicketNo kantar fişi numarası
 * @param shortfallReason deponun eksik yükleme nedeni (stok yetmedi gibi); müdürün kararına girdi olur
 */
public record DispatchMeasurement(
        BigDecimal grossKg,
        BigDecimal tareKg,
        int bagCount,
        @Nullable String scaleTicketNo,
        @Nullable String shortfallReason) {

    private static final int MAX_REASON_LENGTH = 500;

    public DispatchMeasurement(BigDecimal grossKg, BigDecimal tareKg, int bagCount, @Nullable String scaleTicketNo) {
        this(grossKg, tareKg, bagCount, scaleTicketNo, null);
    }

    public DispatchMeasurement {
        Objects.requireNonNull(grossKg, "grossKg");
        Objects.requireNonNull(tareKg, "tareKg");
        if (tareKg.signum() < 0 || grossKg.compareTo(tareKg) < 0) {
            throw new InvalidDispatchException("tareKg", "must be between zero and gross");
        }
        if (bagCount <= 0) {
            throw new InvalidDispatchException("bagCount", "not positive");
        }
        if (shortfallReason != null && (shortfallReason.isBlank() || shortfallReason.length() > MAX_REASON_LENGTH)) {
            throw new InvalidDispatchException("shortfallReason", "blank or too long");
        }
        shortfallReason = shortfallReason == null ? null : shortfallReason.strip();
    }
}
