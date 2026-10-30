package com.yarnshipment.shipment.masterdata.application.port.in.lot;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;

/** Parti listesi; isteyenin lokasyon kapsamına göre süzülür, MASTER_DATA_MANAGE hepsini görür. */
public interface ListLotsUseCase {

    MasterDataPage<ProductionLot> listLots(Actor actor, MasterDataQuery query);
}
