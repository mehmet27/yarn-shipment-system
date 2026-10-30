package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.LockLotQualityUseCase;
import com.yarnshipment.shipment.masterdata.application.port.out.LotEventPort;
import com.yarnshipment.shipment.masterdata.application.port.out.LotPort;
import com.yarnshipment.shipment.masterdata.domain.lot.LotQualityLockedException;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Partinin kalitesini kilitler (rapor Bölüm 10: kalite parti bazında belirlenir ve sevkiyattan önce kilitlenir). Kilit
 * ve {@code LotQualityLocked} olayı aynı transaction'da yazılır; bag-service olayla partiyi çuval bağlanabilir yapar.
 * Kilit geri alınmaz; ikinci kilit isteği {@link LotQualityLockedException} verir.
 */
@Service
public class LockLotQualityService implements LockLotQualityUseCase {

    private final LotPort lots;
    private final LotEventPort events;
    private final LotAccess access;
    private final Clock clock;

    public LockLotQualityService(LotPort lots, LotEventPort events, ScopeGuard scopeGuard, Clock clock) {
        this.lots = lots;
        this.events = events;
        this.access = new LotAccess(lots, scopeGuard);
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProductionLot lockLotQuality(Actor actor, UUID id) {
        ProductionLot lot = access.loadInScope(actor, id);
        lot.requireUnlocked();
        if (!lot.active()) {
            throw new InvalidMasterDataException("lotId", "inactive");
        }
        if (!lots.activeQualityGradeExists(lot.details().qualityGradeId())) {
            throw new InvalidMasterDataException("qualityGradeId", "unknown or inactive");
        }
        if (!lots.lockQuality(actor.userId(), id, clock.instant())) {
            throw new LotQualityLockedException(id);
        }
        ProductionLot locked = access.load(id);
        events.publishQualityLocked(actor.userId(), locked);
        return locked;
    }
}
