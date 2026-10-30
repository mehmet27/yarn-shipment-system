package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.ProductManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.ProductCreateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.ProductUpdateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.ProductView;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ChangeProductStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.CreateProductUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.UpdateProductUseCase;
import com.yarnshipment.shipment.masterdata.domain.catalog.ProductDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** İplik yönetim uçları; yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici (SEC-02: final değildir). */
@RestController
public class ProductManagementController implements ProductManagementApi {

    private final CreateProductUseCase create;
    private final UpdateProductUseCase update;
    private final ChangeProductStatusUseCase status;
    private final CurrentActorProvider currentActorProvider;

    public ProductManagementController(
            CreateProductUseCase create,
            UpdateProductUseCase update,
            ChangeProductStatusUseCase status,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<ProductView> createProduct(UUID idempotencyKey, ProductCreateRequest request) {
        var created = create.createProduct(
                currentActorProvider.getCurrentActor(),
                request.getCode(),
                new ProductDetails(request.getName(), request.getYarnCount()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CatalogPages.view(created));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<ProductView> updateProduct(UUID productId, ProductUpdateRequest request) {
        var updated = update.updateProduct(
                currentActorProvider.getCurrentActor(),
                productId,
                new ProductDetails(request.getName(), request.getYarnCount()));
        return ResponseEntity.ok(CatalogPages.view(updated));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> deactivateProduct(UUID productId) {
        status.changeProductStatus(currentActorProvider.getCurrentActor(), productId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> activateProduct(UUID productId) {
        status.changeProductStatus(currentActorProvider.getCurrentActor(), productId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }
}
