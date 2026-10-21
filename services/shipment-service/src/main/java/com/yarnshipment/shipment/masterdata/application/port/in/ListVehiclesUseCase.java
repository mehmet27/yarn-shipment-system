package com.yarnshipment.shipment.masterdata.application.port.in;

import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;

/** Etkin araçların sayfalı listesi. */
public interface ListVehiclesUseCase {

    MasterDataPage<Vehicle> listVehicles(MasterDataQuery query);
}
