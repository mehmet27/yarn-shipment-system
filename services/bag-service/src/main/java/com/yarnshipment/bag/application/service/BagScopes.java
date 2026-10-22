package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.common.security.scope.ScopedResource;
import java.util.Collection;

/** Çuvalın lokasyonunun isteyenin kapsamında olduğunu denetler (GEN-07). */
final class BagScopes {

    private static final String RESOURCE_TYPE = "Bag";

    private BagScopes() {}

    static void validateAll(ScopeGuard scopeGuard, Actor actor, Collection<Bag> bags, String action) {
        for (Bag bag : bags) {
            scopeGuard.validateInScope(
                    actor, new ScopedResource(RESOURCE_TYPE, bag.id(), bag.siteId(), null, null), action);
        }
    }
}
