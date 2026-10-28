package com.yarnshipment.evidence.domain.ticket;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Fişten okunan bir kilo değeri.
 *
 * @param kg okunan değer; okunamadıysa null
 * @param confidence modelin eminliği
 */
public record WeightValue(@Nullable BigDecimal kg, ReadConfidence confidence) {

    public static final WeightValue MISSING = new WeightValue(null, ReadConfidence.LOW);

    public WeightValue {
        Objects.requireNonNull(confidence, "confidence");
    }

    /** Değer var ve model emin. */
    public boolean isTrusted() {
        return kg != null && confidence == ReadConfidence.HIGH;
    }
}
