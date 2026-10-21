package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;

/** Etkin fasoncuların sayfalı listesi. */
public interface ListSubcontractorsUseCase {

    MasterDataPage<Subcontractor> listSubcontractors(MasterDataQuery query);
}
