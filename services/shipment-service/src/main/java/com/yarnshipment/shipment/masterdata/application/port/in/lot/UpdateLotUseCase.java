package com.yarnshipment.shipment.masterdata.application.port.in.lot;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.util.UUID;

/** Kilitsiz partiyi düzenler; kilitli parti LotQualityLockedException verir. */
public interface UpdateLotUseCase {

    ProductionLot updateLot(Actor actor, UUID id, LotDetails details);
}
