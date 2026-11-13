package com.yarnshipment.events.shipment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * FireOraniDegisti: sevkiyatın fire oranı değiştirildi. Gerekçe serbest metin olduğu için olayda taşınmaz; fire
 * oranı değişiklik tablosunda durur (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param oldRate eski oran
 * @param newRate yeni oran
 * @param changedAt değişiklik zamanı
 */
public record FireRateChanged(UUID shipmentId, BigDecimal oldRate, BigDecimal newRate, Instant changedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.fire-rate-changed.v1";
}
