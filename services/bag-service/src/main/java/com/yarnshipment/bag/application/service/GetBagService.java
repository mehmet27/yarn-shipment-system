package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.BagNotFoundException;
import com.yarnshipment.bag.application.port.in.GetBagQuery;
import com.yarnshipment.bag.application.port.in.GetBagUseCase;
import com.yarnshipment.bag.application.port.out.BagQueryPort;
import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.common.security.scope.ScopeGuard;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Çuvalı Unique Key ile getirir. */
@Service
public class GetBagService implements GetBagUseCase {

    private static final String ACTION = "BAG_VIEW";

    private final BagQueryPort queryPort;
    private final ScopeGuard scopeGuard;

    public GetBagService(BagQueryPort queryPort, ScopeGuard scopeGuard) {
        this.queryPort = queryPort;
        this.scopeGuard = scopeGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public Bag getBag(GetBagQuery query) {
        Bag bag = queryPort
                .findByUniqueKey(query.uniqueKey())
                .orElseThrow(() -> new BagNotFoundException(query.uniqueKey()));
        BagScopes.validateAll(scopeGuard, query.actor(), List.of(bag), ACTION);
        return bag;
    }
}
