package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import java.util.UUID;

/** Ana veri yönetimi: UpdateSite. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface UpdateSiteUseCase {

    Site updateSite(Actor actor, UUID id, String name);
}
