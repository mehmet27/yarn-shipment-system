package com.yarnshipment.reporting.application.port.out;

import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Sorguya eklenecek lokasyon kapsamı.
 *
 * @param siteIds yetkili lokasyonlar; {@code allSites} ise yok sayılır
 * @param allSites lokasyon kısıtı yok
 * @param requestedSiteId istekte istenen lokasyon; kapsamla çakışmışsa servis çözmüştür
 */
public record ReportScope(
        Set<UUID> siteIds, boolean allSites, @Nullable UUID requestedSiteId) {

    public ReportScope {
        siteIds = Set.copyOf(siteIds);
    }
}
