package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import java.util.List;
import java.util.UUID;

/** İplik dönüşü portu; kayıt ve bakiyeden düşen hareket aynı transaction'da yazılır. */
public interface YarnReturnPort {

    /** Sevkiyatın kayıtları, eskiden yeniye. */
    List<YarnReturn> findByShipment(UUID shipmentId);

    void save(YarnReturn yarnReturn);
}
