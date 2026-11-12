package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.util.UUID;

/** Parti olaylarını aynı transaction'da outbox'a yazar (EVT-03). */
public interface LotEventPort {

    /** PartiKalitesiKilitlendi; dış tüketici şu an yoktur, olay outbox'tan yayınlanır. */
    void publishQualityLocked(UUID actorId, ProductionLot lot);
}
