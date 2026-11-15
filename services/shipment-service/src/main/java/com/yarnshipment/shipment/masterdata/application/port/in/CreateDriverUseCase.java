package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;

/** Ana veri yönetimi: CreateDriver. Yalnızca MASTER_DATA_MANAGE yetkisi olan müdür çağırır. */
public interface CreateDriverUseCase {

    Driver createDriver(Actor actor, DriverDetails details);
}
