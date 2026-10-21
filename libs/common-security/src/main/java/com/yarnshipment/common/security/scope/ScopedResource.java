package com.yarnshipment.common.security.scope;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Kapsam kararı için bir kayıttan gereken asgari bilgi. Servisin domain modelinden bağımsızdır (STR-02).
 *
 * @param resourceType kaynak tipi, ör. {@code Shipment}
 * @param resourceId kaynağın kimliği
 * @param siteId kaydın lokasyonu (GEN-07)
 * @param subcontractorId kaydın ait olduğu fasoncu; yoksa {@code null}
 * @param driverId kayda atanmış şoför; yoksa {@code null}
 */
public record ScopedResource(
        String resourceType,
        UUID resourceId,
        UUID siteId,
        @Nullable UUID subcontractorId,
        @Nullable UUID driverId) {

    public ScopedResource {
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(siteId, "siteId");
    }

    /** Audit kaydındaki kaynak gösterimi. */
    public String reference() {
        return resourceType + ":" + resourceId;
    }
}
