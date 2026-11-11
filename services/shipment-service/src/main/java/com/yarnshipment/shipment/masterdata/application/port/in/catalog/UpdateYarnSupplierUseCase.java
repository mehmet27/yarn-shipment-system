package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;
import java.util.UUID;

/** Ana veri yönetimi: UpdateYarnSupplier. Yalnızca MASTER_DATA_MANAGE. */
public interface UpdateYarnSupplierUseCase {

    YarnSupplier updateYarnSupplier(Actor actor, UUID id, String name);
}
