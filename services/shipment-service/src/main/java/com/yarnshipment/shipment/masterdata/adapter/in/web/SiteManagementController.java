package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.SiteManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.SiteRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.SiteView;
import com.yarnshipment.shipment.masterdata.application.port.in.ChangeSiteStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.CreateSiteUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.UpdateSiteUseCase;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Site yönetim uçları; yalnızca MASTER_DATA_MANAGE yetkisi olan yönetici (SEC-02: final değildir). */
@RestController
public class SiteManagementController implements SiteManagementApi {

    private final CreateSiteUseCase create;
    private final UpdateSiteUseCase update;
    private final ChangeSiteStatusUseCase status;
    private final CurrentActorProvider currentActorProvider;

    public SiteManagementController(
            CreateSiteUseCase create,
            UpdateSiteUseCase update,
            ChangeSiteStatusUseCase status,
            CurrentActorProvider currentActorProvider) {
        this.create = create;
        this.update = update;
        this.status = status;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<SiteView> createSite(UUID idempotencyKey, SiteRequest request) {
        var created = create.createSite(currentActorProvider.getCurrentActor(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleSitePages.view(created));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<SiteView> updateSite(UUID siteId, SiteRequest request) {
        var updated = update.updateSite(currentActorProvider.getCurrentActor(), siteId, request.getName());
        return ResponseEntity.ok(VehicleSitePages.view(updated));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> deactivateSite(UUID siteId) {
        status.changeSiteStatus(currentActorProvider.getCurrentActor(), siteId, MasterDataStatus.INACTIVE);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<Void> activateSite(UUID siteId) {
        status.changeSiteStatus(currentActorProvider.getCurrentActor(), siteId, MasterDataStatus.ACTIVE);
        return ResponseEntity.noContent().build();
    }
}
