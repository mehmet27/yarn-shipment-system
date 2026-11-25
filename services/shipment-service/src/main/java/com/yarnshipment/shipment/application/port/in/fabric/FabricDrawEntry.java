package com.yarnshipment.shipment.application.port.in.fabric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Bir sevkiyatın ipliğinden üretilen kumaş: hangi sevkiyatta girilen hangi kumaş için bu sevkiyattan kaç kg iplik
 * düştü.
 *
 * @param fabricId kumaş kaydı
 * @param fabricType kumaş cinsi
 * @param entryShipmentId kumaşın girildiği sevkiyat
 * @param entryShipmentNo kumaşın girildiği sevkiyatın numarası
 * @param yarnType düşülen iplik
 * @param lotNo düşülen ipliğin parti numarası
 * @param netKg düşülen kilo
 * @param receivedAt kumaşın geldiği zaman
 */
public record FabricDrawEntry(
        UUID fabricId,
        String fabricType,
        UUID entryShipmentId,
        String entryShipmentNo,
        String yarnType,
        @Nullable String lotNo,
        BigDecimal netKg,
        Instant receivedAt) {}
