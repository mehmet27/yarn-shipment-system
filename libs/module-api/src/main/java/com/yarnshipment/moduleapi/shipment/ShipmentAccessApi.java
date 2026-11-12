package com.yarnshipment.moduleapi.shipment;

import java.util.UUID;

/** shipment modülünün erişim sorgusu; isteyenin görünürlük ve kapsamı shipment modülünde denetlenir (SEC-01). */
public interface ShipmentAccessApi {

    /**
     * Sevkiyat özeti.
     *
     * @param shipmentId sevkiyat
     * @param siteId çıkış lokasyonu
     * @param status sevkiyat durumu, ör. {@code COMPLETED}
     */
    record ShipmentSummary(UUID shipmentId, UUID siteId, String status) {}

    /**
     * İsteyenin görebildiği sevkiyatı döndürür.
     *
     * @throws ShipmentNotVisibleException sevkiyat yok ya da isteyene görünmüyorsa
     * @throws com.yarnshipment.moduleapi.ModuleUnavailableException modül yanıt vermezse
     */
    ShipmentSummary findVisibleShipment(UUID shipmentId);
}
