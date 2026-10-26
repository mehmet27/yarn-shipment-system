package com.yarnshipment.discrepancy.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Teslimde fasoncunun kör olarak girdiği değerler.
 *
 * @param netKg teslim net kilo
 * @param bagCount teslimde tartılan çuval sayısı
 * @param sealNo teslimde okunan mühür numarası; mühür yoksa boş
 * @param sealCondition mührün durumu
 */
public record DeliveryFigures(
        BigDecimal netKg, int bagCount, @Nullable String sealNo, SealCondition sealCondition) {

    public DeliveryFigures {
        Objects.requireNonNull(netKg, "netKg");
        Objects.requireNonNull(sealCondition, "sealCondition");
    }
}
