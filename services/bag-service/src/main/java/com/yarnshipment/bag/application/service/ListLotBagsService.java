package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.ListLotBagsQuery;
import com.yarnshipment.bag.application.port.in.ListLotBagsUseCase;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.application.port.out.LotQueryPort;
import com.yarnshipment.bag.domain.model.LabeledBag;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Partinin çuvallarını etiket baskı sayılarıyla getirir. */
@Service
public class ListLotBagsService implements ListLotBagsUseCase {

    private static final String ACTION = "BAG_VIEW";
    private static final int LIMIT = 500;

    private final LotQueryPort lots;
    private final BagQueryPort bags;
    private final ScopeGuard scopeGuard;

    public ListLotBagsService(LotQueryPort lots, BagQueryPort bags, ScopeGuard scopeGuard) {
        this.lots = lots;
        this.bags = bags;
        this.scopeGuard = scopeGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabeledBag> listLotBags(ListLotBagsQuery query) {
        LotScopes.validateInScope(scopeGuard, query.actor(), LotScopes.requireLocked(lots, query.lotId()), ACTION);
        return bags.findByLot(query.lotId(), LIMIT);
    }
}
