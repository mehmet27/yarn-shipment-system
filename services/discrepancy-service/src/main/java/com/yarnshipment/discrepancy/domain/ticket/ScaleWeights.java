package com.yarnshipment.discrepancy.domain.ticket;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Bir tartımın kiloları; elle girilen ya da kantar fişinden okunan.
 *
 * @param grossKg brüt
 * @param tareKg dara
 * @param netKg net
 */
public record ScaleWeights(BigDecimal grossKg, BigDecimal tareKg, BigDecimal netKg) {

    public ScaleWeights {
        Objects.requireNonNull(grossKg, "grossKg");
        Objects.requireNonNull(tareKg, "tareKg");
        Objects.requireNonNull(netKg, "netKg");
    }

    @Override
    public String toString() {
        return "gross=" + grossKg.toPlainString() + " tare=" + tareKg.toPlainString() + " net=" + netKg.toPlainString();
    }
}
