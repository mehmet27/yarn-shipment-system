package com.yarnshipment.shipment.domain.correction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Düzeltilen kaydın değeri; düzeltme kaydında eski ve yeni değer {@link #text()} biçimiyle saklanır. */
public sealed interface CorrectedValue {

    String text();

    /**
     * Tartımın brüt ve darası; çuval sayısı ve tartım zamanı eski satırdan aynen taşınır. Değerler üç ondalığa
     * getirilir, böylece {@code 950} ile {@code 950.000} aynı sayılır.
     */
    record Weight(BigDecimal grossKg, BigDecimal tareKg) implements CorrectedValue {

        private static final int SCALE = 3;

        public Weight {
            Objects.requireNonNull(grossKg, "grossKg");
            Objects.requireNonNull(tareKg, "tareKg");
            if (grossKg.stripTrailingZeros().scale() > SCALE) {
                throw new InvalidCorrectionException("grossKg");
            }
            if (tareKg.stripTrailingZeros().scale() > SCALE || tareKg.signum() < 0 || grossKg.compareTo(tareKg) < 0) {
                throw new InvalidCorrectionException("tareKg");
            }
            grossKg = grossKg.setScale(SCALE, RoundingMode.UNNECESSARY);
            tareKg = tareKg.setScale(SCALE, RoundingMode.UNNECESSARY);
        }

        /** Uyuşmazlık kurallarındaki tartı biçimi (discrepancy-service {@code ScaleWeights}). */
        @Override
        public String text() {
            return "gross=%s tare=%s net=%s"
                    .formatted(
                            grossKg.toPlainString(),
                            tareKg.toPlainString(),
                            grossKg.subtract(tareKg).toPlainString());
        }
    }
}
