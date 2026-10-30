package com.yarnshipment.shipment.masterdata.application.port.in.catalog;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
import java.util.UUID;

/** Ana veri yönetimi: UpdateProduct. Kod değişmez; yalnızca MASTER_DATA_MANAGE. */
public interface UpdateProductUseCase {

    Product updateProduct(Actor actor, UUID id, ProductDetails details);
}
