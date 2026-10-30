package com.yarnshipment.shipment.masterdata.application.port.in.lot;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;

/** Partiyi pasife alır veya etkinleştirir; kilitli partide de yapılabilir. */
public interface ChangeLotStatusUseCase {

    void changeLotStatus(Actor actor, UUID id, MasterDataStatus status);
}
