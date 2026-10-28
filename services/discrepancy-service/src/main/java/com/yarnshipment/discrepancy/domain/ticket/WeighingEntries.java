package com.yarnshipment.discrepancy.domain.ticket;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Kantar fişleriyle karşılaştırılan, elle girilmiş değerler.
 *
 * @param dispatch depo çıkış tartımı; girilmediyse boş
 * @param delivery fasoncu teslim tartımı
 * @param plateNo sevkiyatın araç plakası; girilmediyse boş
 */
public record WeighingEntries(
        @Nullable ScaleWeights dispatch,
        ScaleWeights delivery,
        @Nullable String plateNo) {

    public WeighingEntries {
        Objects.requireNonNull(delivery, "delivery");
    }
}
