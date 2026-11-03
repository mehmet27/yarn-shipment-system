package com.yarnshipment.discrepancy.domain.ticket;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Teslim karşılaştırmasına giren, elle girilmiş netler; fiş okumasının yanında gösterilir.
 *
 * @param dispatchNetKg depo çıkış neti
 * @param deliveryNetKg fasoncu teslim neti
 */
public record EnteredNets(BigDecimal dispatchNetKg, BigDecimal deliveryNetKg) {

    public EnteredNets {
        Objects.requireNonNull(dispatchNetKg, "dispatchNetKg");
        Objects.requireNonNull(deliveryNetKg, "deliveryNetKg");
    }

    public BigDecimal of(TicketSide side) {
        return side == TicketSide.DISPATCH_SCALE ? dispatchNetKg : deliveryNetKg;
    }
}
