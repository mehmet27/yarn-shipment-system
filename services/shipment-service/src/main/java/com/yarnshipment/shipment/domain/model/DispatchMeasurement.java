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
 * @param sealNo araca vurulan mühür numarası
 */
public record DispatchMeasurement(
        BigDecimal grossKg,
        BigDecimal tareKg,
        int bagCount,
        @Nullable String scaleTicketNo,
        String sealNo) {

    public DispatchMeasurement {
        Objects.requireNonNull(grossKg, "grossKg");
        Objects.requireNonNull(tareKg, "tareKg");
        Objects.requireNonNull(sealNo, "sealNo");
        if (tareKg.signum() < 0 || grossKg.compareTo(tareKg) < 0) {
            throw new InvalidDispatchException("tareKg", "must be between zero and gross");
        }
        if (bagCount <= 0) {
            throw new InvalidDispatchException("bagCount", "not positive");
        }
        if (sealNo.isBlank()) {
            throw new InvalidDispatchException("sealNo", "blank");
        }
    }
}
