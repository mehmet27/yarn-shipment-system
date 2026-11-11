package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;

/** İplik tedarikçisi listesi; herkese açık referans veri. */
public interface ListYarnSuppliersUseCase {

    MasterDataPage<YarnSupplier> listYarnSuppliers(MasterDataQuery query);
}
