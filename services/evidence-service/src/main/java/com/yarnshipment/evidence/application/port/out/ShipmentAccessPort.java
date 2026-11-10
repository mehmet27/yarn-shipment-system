package com.yarnshipment.evidence.application.port.out;

import java.util.UUID;

/** İsteyenin sevkiyatı görüp göremediğini shipment-service'e sorar (SEC-01). */
public interface ShipmentAccessPort {

    /**
     * Sevkiyat özeti.
     *
     * @param shipmentId sevkiyat
     * @param siteId çıkış lokasyonu
     * @param locked sevkiyat kilitli mi (tamamlanmış ya da iptal edilmiş)
     * @param completed sevkiyat tamamlandı mı; kalite itirazı bundan sonra açılır
     */
    record ShipmentSnapshot(UUID shipmentId, UUID siteId, boolean locked, boolean completed) {}

    /** Sevkiyat isteyene görünmüyorsa {@code ShipmentNotAccessibleException} fırlatır. */
    ShipmentSnapshot findAccessible(UUID shipmentId);
}
