package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.util.UUID;

/** Parti olaylarını aynı transaction'da yayınlar (EVT-03). */
public interface LotEventPort {

    /** PartiKalitesiKilitlendi; dış tüketici şu an yoktur, olay süreç içi yayınlanır. */
    void publishQualityLocked(UUID actorId, ProductionLot lot);
}
