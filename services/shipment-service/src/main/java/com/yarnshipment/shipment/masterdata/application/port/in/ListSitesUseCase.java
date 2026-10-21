package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.model.Site;

/** Yalnızca isteyenin yetkili olduğu lokasyonlar (GEN-07). */
public interface ListSitesUseCase {

    MasterDataPage<Site> listSites(Actor actor, MasterDataQuery query);
}
