package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;

/** Ana veri yönetimi: ChangeSiteStatus. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface ChangeSiteStatusUseCase {

    void changeSiteStatus(Actor actor, UUID id, MasterDataStatus status);
}
