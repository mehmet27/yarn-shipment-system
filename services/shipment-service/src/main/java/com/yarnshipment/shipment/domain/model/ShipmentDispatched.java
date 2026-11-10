package com.yarnshipment.shipment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Depo çıkış yaptı, sevkiyat teslim bekliyor (olay kataloğu: SevkiyatCikisYapti). Kişisel veri taşımaz (EVT-08).
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

    public static ShipmentDispatched of(DispatchRecording recording) {
        return new ShipmentDispatched(
                recording.shipmentId(),
                recording.siteId(),
                recording.subcontractorId(),
                recording.weighing().bagCount(),
                recording.weighing().netKg(),
                recording.recordedAt());
    }
}
