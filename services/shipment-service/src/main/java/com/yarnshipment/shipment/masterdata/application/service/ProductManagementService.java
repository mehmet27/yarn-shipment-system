package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ChangeProductStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.CreateProductUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListProductsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.UpdateProductUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.ProductPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.MasterDataCode;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * İplik listesi, ekleme, güncelleme, pasife alma. Kod eklerken normalleştirilir ve sonra değişmez, çünkü çuvala ve
 * etikete kopyalanır. Yazma yetkisi uçta verilir.
 */
@Service
public class ProductManagementService
        implements ListProductsUseCase, CreateProductUseCase, UpdateProductUseCase, ChangeProductStatusUseCase {

    private static final String KIND = "Product";

    private final ProductPort products;

    public ProductManagementService(ProductPort products) {
        this.products = products;
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<Product> listProducts(MasterDataQuery query) {
        return products.findProducts(query);
    }

    @Override
    @Transactional
    public Product createProduct(Actor actor, String code, ProductDetails details) {
        return products.insertProduct(actor.userId(), MasterDataCode.normalize("code", code), details);
    }

    @Override
    @Transactional
    public Product updateProduct(Actor actor, UUID id, ProductDetails details) {
        return products.updateProduct(actor.userId(), id, details)
                .orElseThrow(() -> new MasterDataNotFoundException(KIND, id));
    }

    @Override
    @Transactional
    public void changeProductStatus(Actor actor, UUID id, MasterDataStatus status) {
        if (!products.setProductStatus(actor.userId(), id, status)) {
            throw new MasterDataNotFoundException(KIND, id);
        }
    }
}
