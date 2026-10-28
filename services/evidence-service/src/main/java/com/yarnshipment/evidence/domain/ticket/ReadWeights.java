package com.yarnshipment.evidence.domain.ticket;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Güvenle okunmuş brüt ve dara.
 *
 * @param grossKg brüt
 * @param tareKg dara
 */
public record ReadWeights(BigDecimal grossKg, BigDecimal tareKg) {

    public ReadWeights {
        Objects.requireNonNull(grossKg, "grossKg");
        Objects.requireNonNull(tareKg, "tareKg");
    }

    /** Brüt eksi dara; fişte basılı net yalnızca kontrol için kullanılır. */
    public BigDecimal netKg() {
        return grossKg.subtract(tareKg);
    }
}
