package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;

/** Ana veri yönetimi: CreateYarnSupplier. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface CreateYarnSupplierUseCase {

    YarnSupplier createYarnSupplier(Actor actor, String name);
}
