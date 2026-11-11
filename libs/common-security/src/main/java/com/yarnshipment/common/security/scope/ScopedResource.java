package com.yarnshipment.common.security.scope;

import java.util.Objects;
import java.util.UUID;

/**
 * Kapsam kararı için bir kayıttan gereken asgari bilgi. Servisin domain modelinden bağımsızdır (STR-02).
 *
 * @param resourceType kaynak tipi, ör. {@code Shipment}
 * @param resourceId kaynağın kimliği
 * @param siteId kaydın lokasyonu (GEN-07)
 */
public record ScopedResource(String resourceType, UUID resourceId, UUID siteId) {

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
