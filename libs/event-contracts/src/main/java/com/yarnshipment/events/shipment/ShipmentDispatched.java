package com.yarnshipment.events.shipment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SevkiyatCikisYapti: depo çıkış tartımını kaydetti, sevkiyat teslim bekliyor. Kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param subcontractorId ipliğin gittiği fasoncu
 * @param bagCount tartılan çuval sayısı
 * @param netKg çıkış net kilosu
 * @param dispatchedAt kayıt zamanı
 */
public record ShipmentDispatched(
        UUID shipmentId, UUID siteId, UUID subcontractorId, int bagCount, BigDecimal netKg, Instant dispatchedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-dispatched.v2";
}
