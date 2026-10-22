package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.util.UUID;

/** Ana veri yönetimi: UpdateVehicle. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface UpdateVehicleUseCase {

    Vehicle updateVehicle(Actor actor, UUID id, String plateNo);
}
