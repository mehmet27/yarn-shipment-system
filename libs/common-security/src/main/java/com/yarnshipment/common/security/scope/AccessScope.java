package com.yarnshipment.common.security.scope;

import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Liste sorgularına eklenecek kapsam filtresi. Repository bunu WHERE koşuluna çevirir; sonuçlar bellekte
 * elenmez (yetkilendirme analizi, "Sahiplik").
 *
 * @param siteIds yalnızca bu lokasyonlar; boş küme lokasyon kısıtı olmadığı anlamına gelir (yalnızca SYSTEM)
 * @param subcontractorId doluysa yalnızca bu fasoncunun kayıtları
 * @param driverId doluysa yalnızca bu şoförün kayıtları
 */
public record AccessScope(
        Set<UUID> siteIds,
        @Nullable UUID subcontractorId,
        @Nullable UUID driverId) {

    /** Kısıtsız kapsam; yalnızca SYSTEM rolüne verilir. */
    public static final AccessScope UNRESTRICTED = new AccessScope(Set.of(), null, null);

    public AccessScope {
        siteIds = Set.copyOf(siteIds);
    }

    public boolean isUnrestricted() {
        return siteIds.isEmpty() && subcontractorId == null && driverId == null;
    }
}
