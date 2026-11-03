package com.yarnshipment.discrepancy.domain.ticket;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Teslim karşılaştırmasına giren, elle girilmiş netler; fiş okumasının yanında gösterilir.
 *
 * @param dispatchNetKg depo çıkış neti; çıkış tartısı girilmemişse boş (QUANTITY_DISPATCH_MISSING)
 * @param deliveryNetKg fasoncu teslim neti
 */
public record EnteredNets(@Nullable BigDecimal dispatchNetKg, BigDecimal deliveryNetKg) {

    public EnteredNets {
        Objects.requireNonNull(deliveryNetKg, "deliveryNetKg");
    }

    public @Nullable BigDecimal of(TicketSide side) {
        return side == TicketSide.DISPATCH_SCALE ? dispatchNetKg : deliveryNetKg;
    }
}
