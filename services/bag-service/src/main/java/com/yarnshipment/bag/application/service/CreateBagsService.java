package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.CreateBagsCommand;
import com.yarnshipment.bag.application.port.in.CreateBagsUseCase;
import com.yarnshipment.bag.application.port.out.BagCreationPort;
import com.yarnshipment.bag.application.port.out.LotQueryPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.LockedLot;
import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kalitesi kilitli partiye çuval üretir. Partinin lokasyonu isteyenin kapsamında olmalıdır. */
@Service
public class CreateBagsService implements CreateBagsUseCase {

    private final LotQueryPort lots;
    private final BagCreationPort creations;
    private final ScopeGuard scopeGuard;
    private final Clock clock;

    public CreateBagsService(LotQueryPort lots, BagCreationPort creations, ScopeGuard scopeGuard, Clock clock) {
        this.lots = lots;
        this.creations = creations;
        this.scopeGuard = scopeGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<Bag> createBags(CreateBagsCommand command) {
        LockedLot lot = LotScopes.requireLocked(lots, command.lotId());
        LotScopes.validateInScope(scopeGuard, command.actor(), lot, Permission.BAG_CREATE.name());
        return creations.create(
                new BagCreationPort.NewBags(lot, command.packageType(), command.count()),
                command.actor().userId(),
                clock.instant());
    }
}
