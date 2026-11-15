package com.yarnshipment.moduleapi.shipment;

import java.time.LocalDate;
import java.util.UUID;

/** shipment modülünün erişim sorgusu; isteyenin görünürlük ve kapsamı shipment modülünde denetlenir (SEC-01). */
public interface ShipmentAccessApi {

    /**
     * Sevkiyat özeti.
     *
     * @param shipmentId sevkiyat
     * @param siteId çıkış lokasyonu
     * @param status sevkiyat durumu, ör. {@code COMPLETED}
     * @param shipmentDate sevkiyat günü (İstanbul); depo bu günden önce işlem yapamaz
     */
    record ShipmentSummary(UUID shipmentId, UUID siteId, String status, LocalDate shipmentDate) {}

    /**
     * İsteyenin görebildiği sevkiyatı döndürür.
     *
     * @throws ShipmentNotVisibleException sevkiyat yok ya da isteyene görünmüyorsa
     * @throws com.yarnshipment.moduleapi.ModuleUnavailableException modül yanıt vermezse
     */
    ShipmentSummary findVisibleShipment(UUID shipmentId);
}
