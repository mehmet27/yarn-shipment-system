package com.yarnshipment.shipment.masterdata.application.port.out;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Ana veri okuma portu. Yalnızca etkin kayıtlar, ada göre sıralı. */
public interface MasterDataQueryPort {

    MasterDataPage<Subcontractor> findSubcontractors(MasterDataQuery query);

    MasterDataPage<Driver> findDrivers(MasterDataQuery query, @Nullable UUID subcontractorId);

    MasterDataPage<Vehicle> findVehicles(MasterDataQuery query);

    /** Kapsam filtresi WHERE koşuluna çevrilir. */
    MasterDataPage<Site> findSites(AccessScope scope, MasterDataQuery query);
}
