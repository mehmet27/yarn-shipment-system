package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.domain.model.NewShipment;
import java.util.UUID;

/** Taslak sevkiyat yazma portu; kayıt, durum geçmişi ve olay tek transaction'dadır. */
public interface ShipmentCreationPort {

    /**
     * Taslağı, ilk durum satırını ve olayı yazar.
     *
     * @param actorUserId işlemi yapan kullanıcı (audit ve geçmiş)
     */
    CreatedShipment create(NewShipment shipment, UUID actorUserId);
}
