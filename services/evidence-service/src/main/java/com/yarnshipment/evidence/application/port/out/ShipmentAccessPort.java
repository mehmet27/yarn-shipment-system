package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.ShipmentNotDueException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** İsteyenin sevkiyatı görüp göremediğini shipment-service'e sorar (SEC-01). */
public interface ShipmentAccessPort {

    /** Fabrikanın günü İstanbul takvimidir. */
    ZoneId SHIPMENT_ZONE = ZoneId.of("Europe/Istanbul");

    /**
     * Sevkiyat özeti.
     *
     * @param shipmentId sevkiyat
     * @param siteId çıkış lokasyonu
     * @param locked sevkiyat kilitli mi (tamamlanmış ya da iptal edilmiş)
     * @param completed sevkiyat tamamlandı mı; kalite itirazı bundan sonra açılır
     * @param shipmentDate sevkiyat günü (İstanbul); depo çıkış kanıtı bu günden önce yüklenemez
     */
    record ShipmentSnapshot(UUID shipmentId, UUID siteId, boolean locked, boolean completed, LocalDate shipmentDate) {

        /** Sevkiyat günü bilinmeyen anlık görüntü: gün her zaman gelmiş sayılır. */
        public ShipmentSnapshot(UUID shipmentId, UUID siteId, boolean locked, boolean completed) {
            this(shipmentId, siteId, locked, completed, LocalDate.MIN);
        }

        /** Sevkiyat günü, verilen anın İstanbul takvim gününden sonraysa gün henüz gelmemiştir (409). */
        public void requireDueAt(Instant now) {
            if (shipmentDate.isAfter(LocalDate.ofInstant(now, SHIPMENT_ZONE))) {
                throw new ShipmentNotDueException(shipmentId, shipmentDate);
            }
        }
    }

    /** Sevkiyat isteyene görünmüyorsa {@code ShipmentNotAccessibleException} fırlatır. */
    ShipmentSnapshot findAccessible(UUID shipmentId);
}
