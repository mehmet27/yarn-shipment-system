package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * Atanan çuvallardan hesaplanan beklenen yük. Kör girişe tabidir; fasoncuya gösterilmez.
 *
 * @param bagCount beklenen çuval sayısı
 * @param netKg beklenen net kilo
 */
public record PlannedLoad(
        @Nullable Integer bagCount, @Nullable BigDecimal netKg) {

    /** Henüz çuval atanmamış sevkiyat. */
    public static final PlannedLoad NONE = new PlannedLoad(null, null);
}
