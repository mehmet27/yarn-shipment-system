package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;

/** İplik listesi; herkese açık referans veri. */
public interface ListProductsUseCase {

    MasterDataPage<Product> listProducts(MasterDataQuery query);
}
