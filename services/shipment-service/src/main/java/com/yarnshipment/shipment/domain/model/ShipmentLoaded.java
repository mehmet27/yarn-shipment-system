package com.yarnshipment.shipment.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Sevkiyat yüklendi, çıkış tartımı ve mühür kaydedildi (olay kataloğu: SevkiyatYuklendi). Çıkış kilosu kör girişe tabi
 * olduğu için olayda yoktur; kişisel veri taşımaz (EVT-08).
 *
 * @param shipmentId sevkiyat
 * @param siteId çıkış lokasyonu
 * @param bagCount tartılan çuval sayısı
 * @param loadedAt kayıt zamanı
 */
public record ShipmentLoaded(UUID shipmentId, UUID siteId, int bagCount, Instant loadedAt) {

    public static final String TYPE = "com.yarnshipment.shipment.shipment-loaded.v1";

    public static ShipmentLoaded of(DispatchRecording recording) {
        return new ShipmentLoaded(
                recording.shipmentId(), recording.siteId(), recording.weighing().bagCount(), recording.recordedAt());
    }
}
