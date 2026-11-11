package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.YarnSupplierManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.YarnSupplierCreateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.YarnSupplierUpdateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.YarnSupplierView;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ChangeYarnSupplierStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.CreateYarnSupplierUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.UpdateYarnSupplierUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** İplik tedarikçisi yönetim uçları; yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici (SEC-02: final değildir). */
@RestController
public class YarnSupplierManagementController implements YarnSupplierManagementApi {

    private final CreateYarnSupplierUseCase create;
    private final UpdateYarnSupplierUseCase update;
    private final ChangeYarnSupplierStatusUseCase status;
    private final CurrentActorProvider currentActorProvider;

    public YarnSupplierManagementController(
            CreateYarnSupplierUseCase create,
            UpdateYarnSupplierUseCase update,
            ChangeYarnSupplierStatusUseCase status,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<YarnSupplierView> createYarnSupplier(UUID idempotencyKey, YarnSupplierCreateRequest request) {
        var created = create.createYarnSupplier(currentActorProvider.getCurrentActor(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(YarnSupplierPages.view(created));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<YarnSupplierView> updateYarnSupplier(UUID yarnSupplierId, YarnSupplierUpdateRequest request) {
        var updated =
                update.updateYarnSupplier(currentActorProvider.getCurrentActor(), yarnSupplierId, request.getName());
        return ResponseEntity.ok(YarnSupplierPages.view(updated));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> deactivateYarnSupplier(UUID yarnSupplierId) {
        status.changeYarnSupplierStatus(
                currentActorProvider.getCurrentActor(), yarnSupplierId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> activateYarnSupplier(UUID yarnSupplierId) {
        status.changeYarnSupplierStatus(
                currentActorProvider.getCurrentActor(), yarnSupplierId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }
}
