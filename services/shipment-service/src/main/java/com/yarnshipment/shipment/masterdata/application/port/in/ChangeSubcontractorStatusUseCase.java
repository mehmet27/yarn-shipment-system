package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;

/** Ana veri yönetimi: ChangeSubcontractorStatus. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface ChangeSubcontractorStatusUseCase {

    void changeSubcontractorStatus(Actor actor, UUID id, MasterDataStatus status);
}
