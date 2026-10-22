package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;

/** Ana veri yönetimi: CreateVehicle. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface CreateVehicleUseCase {

    Vehicle createVehicle(Actor actor, String plateNo);
}
