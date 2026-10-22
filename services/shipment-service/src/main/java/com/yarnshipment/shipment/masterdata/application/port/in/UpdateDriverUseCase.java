package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import java.util.UUID;

/** Ana veri yönetimi: UpdateDriver. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface UpdateDriverUseCase {

    Driver updateDriver(Actor actor, UUID id, DriverDetails details);
}
