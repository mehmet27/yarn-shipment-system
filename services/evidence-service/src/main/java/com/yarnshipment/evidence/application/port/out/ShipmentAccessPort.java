package com.yarnshipment.evidence.application.port.out;

import com.yarnshipment.evidence.domain.model.ShipmentNotDueException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
     * @param shipmentDate sevkiyat günü; depo çıkış fotoğrafı bu günden önce yüklenemez
     * @param departed sevkiyat yola çıktı mı (yolda, tamamlanmış ya da iptal); kantar fişi bundan sonra yenilenmez
     */
    record ShipmentSnapshot(
            UUID shipmentId, UUID siteId, boolean locked, boolean completed, LocalDate shipmentDate, boolean departed) {

        /** Sevkiyat günü fabrikanın takvimine göre sayılır (shipment modülüyle aynı). */
        private static final ZoneId FACTORY_ZONE = ZoneId.of("Europe/Istanbul");

        /** Yola çıkmamış sevkiyat. */
        public ShipmentSnapshot(
                UUID shipmentId, UUID siteId, boolean locked, boolean completed, LocalDate shipmentDate) {
            this(shipmentId, siteId, locked, completed, shipmentDate, false);
        }

        /** Sevkiyat günü gelmediyse {@link ShipmentNotDueException} fırlatır. */
        public void requireDue(Instant now) {
            if (LocalDate.ofInstant(now, FACTORY_ZONE).isBefore(shipmentDate)) {
                throw new ShipmentNotDueException(shipmentId, shipmentDate);
            }
        }
    }

    /** Sevkiyat isteyene görünmüyorsa {@code ShipmentNotAccessibleException} fırlatır. */
    ShipmentSnapshot findAccessible(UUID shipmentId);
}
