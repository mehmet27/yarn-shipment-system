package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;

/** Ana veri yönetimi: CreateProduct. Yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici çağırır. */
public interface CreateProductUseCase {

    Product createProduct(Actor actor, String code, ProductDetails details);
}
