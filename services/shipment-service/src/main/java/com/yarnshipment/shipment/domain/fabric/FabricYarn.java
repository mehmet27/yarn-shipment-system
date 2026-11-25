package com.yarnshipment.shipment.domain.fabric;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kumaşın tükettiği bir iplik: oranı, kilosu ve hangi sevkiyatlardan düşüldüğü.
 *
 * @param yarnType iplik cinsinin adı
 * @param lotId iplik partisi
 * @param lotNo parti numarası, okunurken doludur
 * @param ratioPct kumaşın bu iplikten yapılan yüzdesi
 * @param netKg tüketilen kilo (kumaş kilosu x oran)
 * @param draws düşümün sevkiyatlara dağılımı; toplamı netKg'a eşit
 */
public record FabricYarn(
        String yarnType,
        @Nullable UUID lotId,
        @Nullable String lotNo,
        BigDecimal ratioPct,
        BigDecimal netKg,
        List<YarnDraw> draws) {

    public FabricYarn {
        draws = List.copyOf(draws);
    }
}
