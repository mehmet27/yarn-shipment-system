package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;

/** Ana veri yönetimi: CreateSubcontractor. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface CreateSubcontractorUseCase {

    Subcontractor createSubcontractor(Actor actor, String title);
}
