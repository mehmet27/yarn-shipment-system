package com.yarnshipment.shipment.masterdata.domain.catalog;

import java.util.Objects;
import java.util.UUID;

/**
 * Lokasyon ve ambalaj tipi için standart dara.
 *
 * @param id kimlik
 * @param siteId lokasyon
 * @param siteName lokasyon adı
 * @param packagingType ambalaj tipi
 * @param details dara ve izin verilen sapma
 */
public record TareStandard(
        UUID id, UUID siteId, String siteName, PackagingType packagingType, TareStandardDetails details) {

    public TareStandard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(siteId, "siteId");
        Objects.requireNonNull(siteName, "siteName");
        Objects.requireNonNull(packagingType, "packagingType");
        Objects.requireNonNull(details, "details");
    }
}
