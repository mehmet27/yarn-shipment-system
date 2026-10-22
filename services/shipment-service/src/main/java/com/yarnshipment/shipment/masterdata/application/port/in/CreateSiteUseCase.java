package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Site;

/** Ana veri yönetimi: CreateSite. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface CreateSiteUseCase {

    Site createSite(Actor actor, String name);
}
