package com.yarnshipment.events.shipment;

import java.time.Instant;
import java.util.UUID;

/**
 * FireDosyasiKapandi: fire dosyası kapatıldı. Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param closedAt kapanış zamanı
 */
public record FireSettlementClosed(UUID shipmentId, Instant closedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.fire-settlement-closed.v1";
}
