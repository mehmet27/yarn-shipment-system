package com.yarnshipment.common.security.scope;

import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Liste sorgularına eklenecek kapsam filtresi. Repository bunu WHERE koşuluna çevirir; sonuçlar bellekte
 * elenmez (yetkilendirme analizi, "Sahiplik").
 *
 * @param siteIds yalnızca bu lokasyonlar; {@code allSites} değilse boş küme hiçbir kayıt anlamına gelir
 * @param subcontractorId doluysa yalnızca bu fasoncunun kayıtları
 * @param driverId doluysa yalnızca bu şoförün kayıtları
 * @param allSites lokasyon kısıtı yok; yalnızca {@link #UNRESTRICTED} (SYSTEM) için doğrudur
 */
public record AccessScope(
        Set<UUID> siteIds,
        @Nullable UUID subcontractorId,
        @Nullable UUID driverId,
        boolean allSites) {

    /** Kısıtsız kapsam; yalnızca SYSTEM rolüne verilir. */
    public static final AccessScope UNRESTRICTED = new AccessScope(Set.of(), null, null, true);

    public AccessScope {
        siteIds = Set.copyOf(siteIds);
    }

    /** Lokasyon ve sahiplik kısıtlı kapsam. {@code site_ids} claim'i boş kullanıcı hiçbir kaydı görmez. */
    public static AccessScope restrictedTo(Set<UUID> siteIds, @Nullable UUID subcontractorId, @Nullable UUID driverId) {
        return new AccessScope(siteIds, subcontractorId, driverId, false);
    }

    public boolean isUnrestricted() {
        return allSites && subcontractorId == null && driverId == null;
    }
}
