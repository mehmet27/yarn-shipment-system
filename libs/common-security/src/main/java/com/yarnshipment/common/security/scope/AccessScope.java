package com.yarnshipment.common.security.scope;

import java.util.Set;
import java.util.UUID;

/**
 * Liste sorgularına eklenecek kapsam filtresi. Repository bunu WHERE koşuluna çevirir; sonuçlar bellekte
 * elenmez (yetkilendirme analizi, "Sahiplik").
 *
 * @param siteIds yalnızca bu lokasyonlar; {@code allSites} değilse boş küme hiçbir kayıt anlamına gelir
 * @param allSites lokasyon kısıtı yok; yalnızca {@link #UNRESTRICTED} (SYSTEM) için doğrudur
 */
public record AccessScope(Set<UUID> siteIds, boolean allSites) {

    /** Kısıtsız kapsam; yalnızca SYSTEM rolüne verilir. */
    public static final AccessScope UNRESTRICTED = new AccessScope(Set.of(), true);

    public AccessScope {
        siteIds = Set.copyOf(siteIds);
    }

    /** Lokasyon kısıtlı kapsam. {@code site_ids} claim'i boş kullanıcı hiçbir kaydı görmez. */
    public static AccessScope restrictedTo(Set<UUID> siteIds) {
        return new AccessScope(siteIds, false);
    }

    public boolean isUnrestricted() {
        return allSites;
    }
}
