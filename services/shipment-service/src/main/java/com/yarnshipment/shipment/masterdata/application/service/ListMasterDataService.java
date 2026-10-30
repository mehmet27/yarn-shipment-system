package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.masterdata.application.port.in.ListDriversUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListSitesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListSubcontractorsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.ListVehiclesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.MasterDataQueryPort;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ana veri listeleri. Lokasyonlar isteyenin kapsamına göre süzülür; lokasyonları yöneten ({@code MASTER_DATA_MANAGE})
 * hepsini görür, aksi halde yeni açtığı lokasyon kimsenin {@code site_ids} kapsamında olmadığı için listeye hiç düşmez.
 * Diğer listeler herkese açık referans veridir.
 */
@Service
public class ListMasterDataService
        implements ListSubcontractorsUseCase, ListDriversUseCase, ListVehiclesUseCase, ListSitesUseCase {

    private final MasterDataQueryPort queryPort;
    private final ScopeGuard scopeGuard;

    public ListMasterDataService(MasterDataQueryPort queryPort, ScopeGuard scopeGuard) {
        this.queryPort = queryPort;
        this.scopeGuard = scopeGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<Subcontractor> listSubcontractors(MasterDataQuery query) {
        return queryPort.findSubcontractors(query);
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<Driver> listDrivers(MasterDataQuery query, @Nullable UUID subcontractorId) {
        return queryPort.findDrivers(query, subcontractorId);
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<Vehicle> listVehicles(MasterDataQuery query) {
        return queryPort.findVehicles(query);
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<Site> listSites(Actor actor, MasterDataQuery query) {
        AccessScope scope = actor.hasPermission(Permission.MASTER_DATA_MANAGE)
                ? AccessScope.UNRESTRICTED
                : scopeGuard.scopeOf(actor);
        return queryPort.findSites(scope, query);
    }
}
