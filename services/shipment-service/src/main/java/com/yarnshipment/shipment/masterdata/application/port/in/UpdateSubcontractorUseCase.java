package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import java.util.UUID;

/** Ana veri yönetimi: UpdateSubcontractor. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface UpdateSubcontractorUseCase {

    Subcontractor updateSubcontractor(Actor actor, UUID id, String title);
}
