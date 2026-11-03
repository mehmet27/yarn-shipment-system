package com.yarnshipment.events.shipment;

import java.time.Instant;
import java.util.UUID;

/**
 * SevkiyatYolaCikti: şoför yükü teslim aldı, sevkiyat yolda. Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param driverId şoför
 * @param bagCount onaylanan çuval sayısı
 * @param dispatchedAt beyan zamanı
 */
public record ShipmentDispatched(UUID shipmentId, UUID siteId, UUID driverId, int bagCount, Instant dispatchedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-dispatched.v1";
}
