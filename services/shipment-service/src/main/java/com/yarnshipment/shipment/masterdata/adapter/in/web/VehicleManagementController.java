package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.VehicleManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.VehicleRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.VehicleView;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeVehicleStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateVehicleUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateVehicleUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Vehicle yönetim uçları; yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici (SEC-02: final değildir). */
@RestController
public class VehicleManagementController implements VehicleManagementApi {

    private final CreateVehicleUseCase create;
    private final UpdateVehicleUseCase update;
    private final ChangeVehicleStatusUseCase status;
    private final CurrentActorProvider currentActorProvider;

    public VehicleManagementController(
            CreateVehicleUseCase create,
            UpdateVehicleUseCase update,
            ChangeVehicleStatusUseCase status,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<VehicleView> createVehicle(UUID idempotencyKey, VehicleRequest request) {
        var created = create.createVehicle(currentActorProvider.getCurrentActor(), request.getPlateNo());
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleSitePages.view(created));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<VehicleView> updateVehicle(UUID vehicleId, VehicleRequest request) {
        var updated = update.updateVehicle(currentActorProvider.getCurrentActor(), vehicleId, request.getPlateNo());
        return ResponseEntity.ok(VehicleSitePages.view(updated));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> deactivateVehicle(UUID vehicleId) {
        status.changeVehicleStatus(currentActorProvider.getCurrentActor(), vehicleId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> activateVehicle(UUID vehicleId) {
        status.changeVehicleStatus(currentActorProvider.getCurrentActor(), vehicleId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }
}
