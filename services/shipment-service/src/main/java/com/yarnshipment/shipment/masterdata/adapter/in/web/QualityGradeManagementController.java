package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.QualityGradeManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.QualityGradeCreateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.QualityGradeUpdateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.QualityGradeView;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ChangeQualityGradeStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.CreateQualityGradeUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.UpdateQualityGradeUseCase;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGradeDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Kalite sınıfı yönetim uçları; yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici (SEC-02: final değildir). */
@RestController
public class QualityGradeManagementController implements QualityGradeManagementApi {

    private final CreateQualityGradeUseCase create;
    private final UpdateQualityGradeUseCase update;
    private final ChangeQualityGradeStatusUseCase status;
    private final CurrentActorProvider currentActorProvider;

    public QualityGradeManagementController(
            CreateQualityGradeUseCase create,
            UpdateQualityGradeUseCase update,
            ChangeQualityGradeStatusUseCase status,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<QualityGradeView> createQualityGrade(UUID idempotencyKey, QualityGradeCreateRequest request) {
        var created = create.createQualityGrade(
                currentActorProvider.getCurrentActor(),
                request.getCode(),
                new QualityGradeDetails(request.getName(), request.getLabelColor()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CatalogPages.view(created));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<QualityGradeView> updateQualityGrade(UUID qualityGradeId, QualityGradeUpdateRequest request) {
        var updated = update.updateQualityGrade(
                currentActorProvider.getCurrentActor(),
                qualityGradeId,
                new QualityGradeDetails(request.getName(), request.getLabelColor()));
        return ResponseEntity.ok(CatalogPages.view(updated));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> deactivateQualityGrade(UUID qualityGradeId) {
        status.changeQualityGradeStatus(
                currentActorProvider.getCurrentActor(), qualityGradeId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> activateQualityGrade(UUID qualityGradeId) {
        status.changeQualityGradeStatus(
                currentActorProvider.getCurrentActor(), qualityGradeId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }
}
