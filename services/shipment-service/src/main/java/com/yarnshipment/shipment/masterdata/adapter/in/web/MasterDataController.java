package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.MasterDataApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.DriverPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.SitePageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.SubcontractorPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.VehiclePageResponse;
import com.yarnshipment.shipment.masterdata.application.port.in.ListDriversUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListSitesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListSubcontractorsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListVehiclesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ana veri okuma uçları. Ayrı master-data servisi gelene kadar burada, kendi paketinde durur. Method security
 * proxy'si gerektirdiği için final değildir (SEC-02).
 */
@RestController
public class MasterDataController implements MasterDataApi {

    private final ListSubcontractorsUseCase listSubcontractors;
    private final ListDriversUseCase listDrivers;
    private final ListVehiclesUseCase listVehicles;
    private final ListSitesUseCase listSites;
    private final CurrentActorProvider currentActorProvider;

    public MasterDataController(
            ListSubcontractorsUseCase listSubcontractors,
            ListDriversUseCase listDrivers,
            ListVehiclesUseCase listVehicles,
            ListSitesUseCase listSites,
            CurrentActorProvider currentActorProvider) {
        this.listSubcontractors = listSubcontractors;
        this.listDrivers = listDrivers;
        this.listVehicles = listVehicles;
        this.listSites = listSites;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW')")
    public ResponseEntity<SubcontractorPageResponse> listSubcontractors(
            Integer page, Integer size, @Nullable String search) {
        return ResponseEntity.ok(MasterDataPages.subcontractors(
                listSubcontractors.listSubcontractors(new MasterDataQuery(search, page, size))));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW')")
    public ResponseEntity<DriverPageResponse> listDrivers(
            Integer page, Integer size, @Nullable String search, @Nullable UUID subcontractorId) {
        return ResponseEntity.ok(MasterDataPages.drivers(
                listDrivers.listDrivers(new MasterDataQuery(search, page, size), subcontractorId)));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW')")
    public ResponseEntity<VehiclePageResponse> listVehicles(Integer page, Integer size, @Nullable String search) {
        return ResponseEntity.ok(
                VehicleSitePages.vehicles(listVehicles.listVehicles(new MasterDataQuery(search, page, size))));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW')")
    public ResponseEntity<SitePageResponse> listSites(Integer page, Integer size, @Nullable String search) {
        return ResponseEntity.ok(VehicleSitePages.sites(
                listSites.listSites(currentActorProvider.getCurrentActor(), new MasterDataQuery(search, page, size))));
    }
}
