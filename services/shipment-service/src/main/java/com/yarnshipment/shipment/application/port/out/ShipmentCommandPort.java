package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.PickupConfirmation;
import java.util.UUID;

/** Sevkiyat yazma portu. Her metot durum geçişini, geçmiş satırını ve outbox olayını aynı transaction'da yazar. */
public interface ShipmentCommandPort {

    /** Sevkiyat için teslim alma beyanı daha önce kaydedildi mi (yeniden deneme). */
    boolean hasPickupConfirmation(UUID shipmentId);

    /**
     * Beyanı kaydeder ve durumu IN_TRANSIT yapar. Arada durum değiştiyse {@code InvalidShipmentStatusException}.
     *
     * @param actorUserId işlemi yapan kullanıcı (audit ve geçmiş)
     */
    void recordPickup(PickupConfirmation confirmation, UUID actorUserId);
}
