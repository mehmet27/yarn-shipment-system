package com.yarnshipment.shipment.domain.fabric;

import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kumaşın bir sevkiyatın ipliğinden aldığı kilo.
 *
 * @param shipmentId ipliği düşülen sevkiyat
 * @param shipmentNo sevkiyat numarası, okunurken doludur
 * @param rawYarnType defterde yazılı iplik anahtarı
 * @param lotId iplik partisi
 * @param netKg düşülen kilo
 */
public record YarnDraw(
        UUID shipmentId,
        @Nullable String shipmentNo,
        String rawYarnType,
        @Nullable UUID lotId,
        BigDecimal netKg) {}
