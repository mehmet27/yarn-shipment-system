package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.LotManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.LotCreateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.LotUpdateRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.LotView;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.ChangeLotStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.CreateLotUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.LockLotQualityUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.UpdateLotUseCase;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parti ve kalite kilidi uçları; LOT_QUALITY_LOCK yetkisi olan kalite kullanıcısı ve yönetici. Lokasyon kapsamı
 * serviste denetlenir (SEC-02: final değildir).
 */
@RestController
public class LotManagementController implements LotManagementApi {

    private static final String LOT_ACCESS = "hasAuthority('LOT_QUALITY_LOCK')";

    private final CreateLotUseCase create;
    private final UpdateLotUseCase update;
    private final ChangeLotStatusUseCase status;
    private final LockLotQualityUseCase lock;
    private final CurrentActorProvider currentActorProvider;

    public LotManagementController(
            CreateLotUseCase create,
            UpdateLotUseCase update,
            ChangeLotStatusUseCase status,
            LockLotQualityUseCase lock,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.lock = lock;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize(LOT_ACCESS)
    public ResponseEntity<LotView> createLot(UUID idempotencyKey, LotCreateRequest request) {
        var details = new LotDetails(
                request.getLotNo(), request.getProductId(), request.getQualityGradeId(), request.getProductionDate());
        var created = create.createLot(currentActorProvider.getCurrentActor(), request.getSiteId(), details);
        return ResponseEntity.status(HttpStatus.CREATED).body(CatalogPages.view(created));
    }

    @Override
    @PreAuthorize(LOT_ACCESS)
    public ResponseEntity<LotView> updateLot(UUID lotId, LotUpdateRequest request) {
        var details = new LotDetails(
                request.getLotNo(), request.getProductId(), request.getQualityGradeId(), request.getProductionDate());
        return ResponseEntity.ok(
                CatalogPages.view(update.updateLot(currentActorProvider.getCurrentActor(), lotId, details)));
    }

    @Override
    @PreAuthorize(LOT_ACCESS)
    public ResponseEntity<Void> deactivateLot(UUID lotId) {
        status.changeLotStatus(currentActorProvider.getCurrentActor(), lotId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize(LOT_ACCESS)
    public ResponseEntity<Void> activateLot(UUID lotId) {
        status.changeLotStatus(currentActorProvider.getCurrentActor(), lotId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize(LOT_ACCESS)
    public ResponseEntity<LotView> lockLotQuality(UUID lotId) {
        return ResponseEntity.ok(CatalogPages.view(lock.lockLotQuality(currentActorProvider.getCurrentActor(), lotId)));
    }
}
