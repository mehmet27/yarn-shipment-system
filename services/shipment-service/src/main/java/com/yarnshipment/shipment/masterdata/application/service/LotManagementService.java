package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.ChangeLotStatusUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.CreateLotUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.ListLotsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.UpdateLotUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.LotPort;
import com.yarnshipment.shipment.masterdata.domain.lot.LotDetails;
import com.yarnshipment.shipment.masterdata.domain.lot.LotQualityLockedException;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parti listesi, ekleme, düzenleme ve pasife alma (rapor Bölüm 10). Parti kilitsiz açılır; kalitesi kilitlenene kadar
 * numarası, ipliği, kalitesi ve üretim tarihi düzeltilebilir, kilitten sonra yalnızca pasife alınabilir. Kilit
 * {@link LockLotQualityService}'tedir.
 */
@Service
public class LotManagementService
        implements ListLotsUseCase, CreateLotUseCase, UpdateLotUseCase, ChangeLotStatusUseCase {

    private final LotPort lots;
    private final LotAccess access;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public LotManagementService(LotPort lots, ScopeGuard scopeGuard, Clock clock) {
        this.lots = lots;
        this.access = new LotAccess(lots, scopeGuard);
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public MasterDataPage<ProductionLot> listLots(Actor actor, MasterDataQuery query) {
        AccessScope scope = actor.hasPermission(Permission.MASTER_DATA_MANAGE)
                ? AccessScope.UNRESTRICTED
                : scopeGuard.scopeOf(actor);
        return lots.findLots(scope, query);
    }

    @Override
    @Transactional
    public ProductionLot createLot(Actor actor, UUID siteId, LotDetails details) {
        access.requireSiteInScope(actor, siteId);
        if (!lots.activeSiteExists(siteId)) {
            throw new InvalidMasterDataException("siteId", "unknown or inactive");
        }
        requireValidReferences(details);
        return access.load(lots.insertLot(actor.userId(), siteId, details));
    }

    @Override
    @Transactional
    public ProductionLot updateLot(Actor actor, UUID id, LotDetails details) {
        access.loadInScope(actor, id).requireUnlocked();
        requireValidReferences(details);
        if (!lots.updateLot(actor.userId(), id, details)) {
            throw new LotQualityLockedException(id);
        }
        return access.load(id);
    }

    @Override
    @Transactional
    public void changeLotStatus(Actor actor, UUID id, MasterDataStatus status) {
        access.loadInScope(actor, id);
        lots.setLotStatus(actor.userId(), id, status);
    }

    private void requireValidReferences(LotDetails details) {
        if (!lots.activeProductExists(details.productId())) {
            throw new InvalidMasterDataException("productId", "unknown or inactive");
        }
        if (!lots.activeQualityGradeExists(details.qualityGradeId())) {
            throw new InvalidMasterDataException("qualityGradeId", "unknown or inactive");
        }
        if (details.productionDate().isAfter(LocalDate.now(clock))) {
            throw new InvalidMasterDataException("productionDate", "in the future");
        }
    }
}
