package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.SubcontractorManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.LoginAccountResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.SubcontractorRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.SubcontractorView;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeSubcontractorStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorLoginAccountUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateSubcontractorUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Fasoncu yönetim uçları; yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici (SEC-02: final değildir). */
@RestController
public class SubcontractorManagementController implements SubcontractorManagementApi {

    private final CreateSubcontractorUseCase create;
    private final UpdateSubcontractorUseCase update;
    private final ChangeSubcontractorStatusUseCase status;
    private final CreateSubcontractorLoginAccountUseCase loginAccount;
    private final CurrentActorProvider currentActorProvider;

    public SubcontractorManagementController(
            CreateSubcontractorUseCase create,
            UpdateSubcontractorUseCase update,
            ChangeSubcontractorStatusUseCase status,
            CreateSubcontractorLoginAccountUseCase loginAccount,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.loginAccount = loginAccount;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<SubcontractorView> createSubcontractor(UUID idempotencyKey, SubcontractorRequest request) {
        var created = create.createSubcontractor(currentActorProvider.getCurrentActor(), request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(MasterDataPages.view(created));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<SubcontractorView> updateSubcontractor(UUID subcontractorId, SubcontractorRequest request) {
        var updated =
                update.updateSubcontractor(currentActorProvider.getCurrentActor(), subcontractorId, request.getTitle());
        return ResponseEntity.ok(MasterDataPages.view(updated));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> deactivateSubcontractor(UUID subcontractorId) {
        status.changeSubcontractorStatus(
                currentActorProvider.getCurrentActor(), subcontractorId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> activateSubcontractor(UUID subcontractorId) {
        status.changeSubcontractorStatus(
                currentActorProvider.getCurrentActor(), subcontractorId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<LoginAccountResponse> createSubcontractorLoginAccount(
            UUID subcontractorId, LoginAccountRequest request) {
        var account = loginAccount.createSubcontractorLoginAccount(
                currentActorProvider.getCurrentActor(), subcontractorId, LoginAccountViews.command(request));
        return LoginAccountViews.created(account);
    }
}
