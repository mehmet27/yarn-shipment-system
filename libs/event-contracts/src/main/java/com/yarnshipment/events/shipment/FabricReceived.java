package com.yarnshipment.events.shipment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * KumasGirisiYapildi: fasoncudan gelen kumaş sevkiyata kaydedildi. Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param receiptId kumaş kaydı
 * @param fabricType kumaş cinsi
 * @param netKg gelen kumaş net kg
 * @param receivedAt kumaşın geliş zamanı
 */
public record FabricReceived(UUID shipmentId, UUID receiptId, String fabricType, BigDecimal netKg, Instant receivedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.fabric-received.v1";
}
