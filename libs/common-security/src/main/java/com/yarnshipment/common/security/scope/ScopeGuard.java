package com.yarnshipment.common.security.scope;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;
import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.audit.SecurityAuditEvent;
import java.time.Clock;
import java.util.Objects;

/**
 * Kapsam kontrolü (SEC-03): kullanıcılar
 * yalnızca yetkili oldukları lokasyonlara erişir. Kayıt yüklendikten sonra çağrılır; kapsam dışı erişim 403 döner ve
 * audit'e yazılır. Duruma bağlı kurallar ilgili servistedir.
 */
public final class ScopeGuard {

    private final AuditPublisher auditPublisher;
    private final Clock clock;

    public ScopeGuard(AuditPublisher auditPublisher, Clock clock) {
        this.auditPublisher = Objects.requireNonNull(auditPublisher, "auditPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Aktörün liste sorgularına uygulanacak kapsamı. */
    public AccessScope scopeOf(Actor actor) {
        if (actor.hasRole(Role.SYSTEM)) {
            return AccessScope.UNRESTRICTED;
        }
        return AccessScope.restrictedTo(actor.siteIds());
    }

    public boolean isInScope(Actor actor, ScopedResource resource) {
        AccessScope scope = scopeOf(actor);
        if (scope.isUnrestricted()) {
            return true;
        }
        return isSiteAllowed(scope, resource);
    }

    /** Kapsam dışındaysa reddi audit'e yazar ve {@link OutOfScopeAccessException} fırlatır. */
    public void validateInScope(Actor actor, ScopedResource resource, String action) {
        if (isInScope(actor, resource)) {
            return;
        }
        auditPublisher.publish(SecurityAuditEvent.denied(clock.instant(), actor, action, resource.reference())
                .withReason("OUT_OF_SCOPE"));
        throw new OutOfScopeAccessException(resource.reference());
    }

    /** Lokasyon kapsamı: yalnızca SYSTEM lokasyondan bağımsızdır; site_ids claim'i boş kullanıcı hiçbir şey görmez. */
    private static boolean isSiteAllowed(AccessScope scope, ScopedResource resource) {
        return scope.siteIds().contains(resource.siteId());
    }
}
