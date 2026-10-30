package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.util.UUID;

/** Parti olaylarını aynı transaction'da outbox'a yazar (EVT-03). */
public interface LotEventPort {

    /** PartiKalitesiKilitlendi; bag-service çuvalın bağlanabileceği partiyi bununla öğrenir. */
    void publishQualityLocked(UUID actorId, ProductionLot lot);
}
