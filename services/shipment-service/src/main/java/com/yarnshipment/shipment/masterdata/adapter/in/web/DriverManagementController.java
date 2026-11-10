package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.DriverManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.DriverRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.DriverView;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeDriverStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateDriverUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateDriverUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Şoför yönetim uçları; yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici (SEC-02: final değildir). */
@RestController
public class DriverManagementController implements DriverManagementApi {

    private final CreateDriverUseCase create;
    private final UpdateDriverUseCase update;
    private final ChangeDriverStatusUseCase status;
    private final CurrentActorProvider currentActorProvider;

    public DriverManagementController(
            CreateDriverUseCase create,
            UpdateDriverUseCase update,
            ChangeDriverStatusUseCase status,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<DriverView> createDriver(UUID idempotencyKey, DriverRequest request) {
        var created = create.createDriver(currentActorProvider.getCurrentActor(), details(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(MasterDataPages.view(created));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<DriverView> updateDriver(UUID driverId, DriverRequest request) {
        var updated = update.updateDriver(currentActorProvider.getCurrentActor(), driverId, details(request));
        return ResponseEntity.ok(MasterDataPages.view(updated));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> deactivateDriver(UUID driverId) {
        status.changeDriverStatus(currentActorProvider.getCurrentActor(), driverId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> activateDriver(UUID driverId) {
        status.changeDriverStatus(currentActorProvider.getCurrentActor(), driverId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }

    private static DriverDetails details(DriverRequest request) {
        return new DriverDetails(request.getFullName(), request.getSubcontractorId());
    }
}
